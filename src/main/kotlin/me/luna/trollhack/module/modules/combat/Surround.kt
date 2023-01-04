package me.luna.trollhack.module.modules.combat

import it.unimi.dsi.fastutil.longs.Long2LongMaps
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSets
import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.event.events.RunGameLoopEvent
import me.luna.trollhack.event.events.StepEvent
import me.luna.trollhack.event.events.TickEvent
import me.luna.trollhack.event.events.WorldEvent
import me.luna.trollhack.event.events.combat.CrystalSetDeadEvent
import me.luna.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import me.luna.trollhack.event.events.player.PlayerMoveEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.manager.managers.CombatManager
import me.luna.trollhack.manager.managers.EntityManager
import me.luna.trollhack.manager.managers.HoleManager
import me.luna.trollhack.manager.managers.HotbarManager.spoofHotbar
import me.luna.trollhack.manager.managers.PlayerPacketManager
import me.luna.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.module.modules.movement.AutoCenter
import me.luna.trollhack.util.EntityUtils.betterPosition
import me.luna.trollhack.util.MovementUtils.isCentered
import me.luna.trollhack.util.MovementUtils.realSpeed
import me.luna.trollhack.util.TickTimer
import me.luna.trollhack.util.TimeUnit
import me.luna.trollhack.util.atTrue
import me.luna.trollhack.util.collections.EnumMap
import me.luna.trollhack.util.combat.CrystalUtils
import me.luna.trollhack.util.combat.HoleType
import me.luna.trollhack.util.extension.synchronized
import me.luna.trollhack.util.inventory.slot.HotbarSlot
import me.luna.trollhack.util.inventory.slot.firstBlock
import me.luna.trollhack.util.inventory.slot.hotbarSlots
import me.luna.trollhack.util.math.RotationUtils.getRotationTo
import me.luna.trollhack.util.math.isInSight
import me.luna.trollhack.util.text.MessageSendUtils
import me.luna.trollhack.util.threads.onMainThreadSafe
import me.luna.trollhack.util.threads.runSynchronized
import me.luna.trollhack.util.world.*
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

@CombatManager.CombatModule
internal object Surround : Module(
    name = "Surround",
    category = Category.COMBAT,
    description = "Surrounds you with obsidian to take less damage",
    modulePriority = 200
) {
    private val placeDelay by setting("Place delay", 50, 0..1000, 1)
    private val multiPlace by setting("Multi Place", 2, 1..5, 1)
    private val placeTimeout by setting("Place Timeout", 200, 0..1000, 10)
    private val strictDirection by setting("Strict Direction", false)
    private val autoCenter by setting("Auto Center", true)
    private val rotation by setting("Rotation", true)
    private val autoDisable0 = setting("Auto Disable", AutoDisableMode.OUT_OF_HOLE)
    private val autoDisable by autoDisable0
    private val enableInHole0 = setting("Enable In Hole", false)
    private val enableInHole by enableInHole0
    private val inHoleTimeout by setting("In Hole Timeout", 50, 1..100, 5, enableInHole0.atTrue(), description = "Delay before enabling Surround when you are in hole, in ticks")

    private enum class AutoDisableMode {
        NEVER, ONE_TIME, OUT_OF_HOLE
    }

    private val toggleTimer = TickTimer(TimeUnit.TICKS)
    private val placeTimer = TickTimer()

    private val placing = EnumMap<SurroundOffset, List<PlaceInfo>>().synchronized()
    private val placingSet = LongOpenHashSet()
    private val pendingPlacing = Long2LongMaps.synchronize(Long2LongOpenHashMap()).apply { defaultReturnValue(-1L) }
    private val placed = LongSets.synchronize(LongOpenHashSet())

    private var holePos: BlockPos? = null
    private var enableTicks = 0

    override fun isActive(): Boolean {
        return isEnabled && placing.isNotEmpty()
    }

    init {
        onEnable {
            HolePathFinder.disable()
        }

        onDisable {
            placeTimer.reset(-114514L)
            toggleTimer.reset()

            placing.clear()
            placingSet.clear()
            pendingPlacing.clear()
            placed.clear()

            holePos = null
            enableTicks = 0
        }

        safeListener<CrystalSetDeadEvent> { event ->
            if (event.crystals.none { it.getDistanceSq(player) < 6.0 }) return@safeListener
            var placeCount = 0

            placing.runSynchronized {
                val iterator = values.iterator()
                while (iterator.hasNext()) {
                    val list = iterator.next()
                    var allPlaced = true

                    loop@ for (placeInfo in list) {
                        if (event.crystals.none { CrystalUtils.placeBoxIntersectsCrystalBox(placeInfo.placedPos, it) }) continue

                        val long = placeInfo.placedPos.toLong()
                        if (placed.contains(long)) continue
                        allPlaced = false

                        if (System.currentTimeMillis() <= pendingPlacing[long]) continue
                        if (!checkRotation(placeInfo)) continue

                        placeBlock(placeInfo)
                        placeCount++
                        if (placeCount >= multiPlace) return@safeListener
                    }

                    if (allPlaced) iterator.remove()
                }
            }

            if (autoDisable == AutoDisableMode.ONE_TIME && placing.isEmpty()) {
                disable()
            }
        }

        safeListener<WorldEvent.ServerBlockUpdate> { event ->
            val pos = event.pos
            if (!event.newState.isReplaceable) {
                val long = pos.toLong()
                if (placingSet.contains(long)) {
                    pendingPlacing.remove(long)
                    placed.add(long)
                }
            } else {
                val relative = pos.subtract(player.betterPosition)
                if (SurroundOffset.values().any { it.offset == relative } && checkColliding(pos)) {
                    getNeighbor(pos)?.let { placeInfo ->
                        if (checkRotation(placeInfo)) {
                            placingSet.add(placeInfo.placedPos.toLong())
                            placeBlock(placeInfo)
                        }
                    }
                }
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (rotation) {
                placing.runSynchronized {
                    for (list in values) {
                        for (placeInfo in list) {
                            val long = placeInfo.placedPos.toLong()
                            if (placed.contains(long)) {
                                continue
                            }

                            sendPlayerPacket {
                                var eyeHeight = player.getEyeHeight()
                                if (!player.isSneaking) eyeHeight -= 0.08f
                                rotate(getRotationTo(Vec3d(player.posX, player.posY + eyeHeight, player.posZ), placeInfo.hitVec))
                            }
                            return@safeListener
                        }
                    }
                }
            }
        }

        safeListener<TickEvent.Pre> {
            enableTicks++
        }

        listener<StepEvent> {
            if (autoDisable == AutoDisableMode.NEVER) {
                placing.clear()
                placingSet.clear()
                pendingPlacing.clear()
                placed.clear()
                holePos = null
            } else {
                disable()
            }
        }

        safeListener<RunGameLoopEvent.Tick>(true) {
            if (!player.onGround) {
                if (isEnabled) disable()
                return@safeListener
            }

            var playerPos = player.betterPosition
            val isInHole = player.onGround && player.realSpeed < 0.1 && HoleManager.getHoleInfo(playerPos).type == HoleType.OBBY

            if (isDisabled) {
                enableInHoleCheck(isInHole)
                return@safeListener
            }

            if (world.getBlockState(playerPos.down()).getCollisionBoundingBox(world, playerPos) == null) {
                playerPos = world.getGroundPos(player).up()
            }

            if (isInHole || holePos == null) {
                holePos = playerPos
            }

            // Out of hole check
            if (HolePathFinder.isActive()) {
                if (autoDisable == AutoDisableMode.NEVER) {
                    placing.clear()
                    placingSet.clear()
                    pendingPlacing.clear()
                    placed.clear()
                    holePos = null
                } else {
                    disable()
                }

                return@safeListener
            }

            updatePlacingMap(playerPos)

            if (placing.isNotEmpty() && placeTimer.tickAndReset(placeDelay)) {
                runPlacing()
            }
        }

        safeListener<PlayerMoveEvent.Pre> {
            if (autoCenter) {
                val holePos = holePos
                if (holePos != null) {
                    if (!player.isCentered(holePos) && (placing.isNotEmpty() || isSurroundPlaceable(holePos))) {
                        AutoCenter.centerPlayer(holePos)
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.isSurroundPlaceable(holePos: BlockPos): Boolean {
        for (surroundOffset in SurroundOffset.values()) {
            val offsetPos = holePos.add(surroundOffset.offset)
            if (world.getBlockState(offsetPos).isReplaceable) return true
        }

        return false
    }

    private fun enableInHoleCheck(isInHole: Boolean) {
        if (enableInHole && isInHole) {
            if (toggleTimer.tickAndReset(inHoleTimeout)) {
                enable()
            }
        } else {
            toggleTimer.reset()
        }
    }

    private fun SafeClientEvent.updatePlacingMap(playerPos: BlockPos) {
        pendingPlacing.runSynchronized {
            keys.removeIf {
                if (!world.getBlockState(BlockPos.fromLong(it)).isReplaceable) {
                    placed.add(it)
                } else {
                    false
                }
            }
        }

        if (placing.isEmpty() && (pendingPlacing.isEmpty() || pendingPlacing.runSynchronized { values.all { System.currentTimeMillis() > it } })) {
            placing.clear()
            placed.clear()
        }

        for (surroundOffset in SurroundOffset.values()) {
            val offsetPos = playerPos.add(surroundOffset.offset)
            if (!world.getBlockState(offsetPos).isReplaceable) continue

            getNeighborSequence(offsetPos, 2, 5.0f, strictDirection, false)?.let { list ->
                placing[surroundOffset] = list
                list.forEach {
                    placingSet.add(it.placedPos.toLong())
                }
            }
        }
    }

    private fun SafeClientEvent.runPlacing() {
        var placeCount = 0

        placing.runSynchronized {
            val iterator = placing.values.iterator()
            while (iterator.hasNext()) {
                val list = iterator.next()
                var allPlaced = true
                var breakCrystal = false

                loop@ for (placeInfo in list) {
                    val long = placeInfo.placedPos.toLong()
                    if (placed.contains(long)) continue
                    allPlaced = false

                    if (System.currentTimeMillis() <= pendingPlacing[long]) continue
                    if (!checkRotation(placeInfo)) continue

                    for (entity in EntityManager.entity) {
                        if (breakCrystal && entity is EntityEnderCrystal) continue
                        if (!entity.preventEntitySpawning) continue
                        if (!entity.isEntityAlive) continue
                        if (!entity.entityBoundingBox.intersects(AxisAlignedBB(placeInfo.placedPos))) continue
                        if (entity !is EntityEnderCrystal) continue@loop

                        connection.sendPacket(CPacketUseEntity(entity))
                        connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
                        breakCrystal = true
                    }

                    placeBlock(placeInfo)
                    placeCount++
                    if (placeCount >= multiPlace) return
                }

                if (allPlaced) iterator.remove()
            }
        }

        if (autoDisable == AutoDisableMode.ONE_TIME && placing.isEmpty()) {
            disable()
        }
    }

    private fun SafeClientEvent.getNeighbor(pos: BlockPos): PlaceInfo? {
        for (side in EnumFacing.values()) {
            val offsetPos = pos.offset(side)
            val oppositeSide = side.opposite

            if (strictDirection && !getVisibleSides(offsetPos, true).contains(oppositeSide)) continue
            if (world.getBlockState(offsetPos).isReplaceable) continue

            val hitVec = getHitVec(offsetPos, oppositeSide)
            val hitVecOffset = getHitVecOffset(oppositeSide)

            return PlaceInfo(offsetPos, oppositeSide, 0.0, hitVecOffset, hitVec, pos)
        }

        return null
    }

    private fun checkColliding(pos: BlockPos): Boolean {
        val box = AxisAlignedBB(pos)

        return EntityManager.entity.none {
            it.isEntityAlive && it.preventEntitySpawning && it.entityBoundingBox.intersects(box)
        }
    }

    private fun SafeClientEvent.placeBlock(placeInfo: PlaceInfo) {
        val slot = getSlot() ?: run {
            disable()
            return
        }

        val sneak = !player.isSneaking
        if (sneak) connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))

        spoofHotbar(slot) {
            connection.sendPacket(placeInfo.toPlacePacket(EnumHand.MAIN_HAND))
        }
        connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))

        if (sneak) connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))

        onMainThreadSafe {
            val blockState = Blocks.OBSIDIAN.getStateForPlacement(world, placeInfo.pos, placeInfo.side, placeInfo.hitVecOffset.x, placeInfo.hitVecOffset.y, placeInfo.hitVecOffset.z, 0, player, EnumHand.MAIN_HAND)
            val soundType = blockState.block.getSoundType(blockState, world, placeInfo.pos, player)
            world.playSound(player, placeInfo.pos, soundType.placeSound, SoundCategory.BLOCKS, (soundType.getVolume() + 1.0f) / 2.0f, soundType.getPitch() * 0.8f)
        }

        pendingPlacing[placeInfo.placedPos.toLong()] = System.currentTimeMillis() + placeTimeout
    }

    private fun SafeClientEvent.getSlot(): HotbarSlot? {
        val slot = player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)

        return if (slot == null) {
            MessageSendUtils.sendNoSpamChatMessage("$chatName No obsidian in hotbar!")
            null
        } else {
            slot
        }
    }

    private fun SafeClientEvent.checkRotation(placeInfo: PlaceInfo): Boolean {
        var eyeHeight = player.getEyeHeight()
        if (!player.isSneaking) eyeHeight -= 0.08f
        return !rotation || AxisAlignedBB(placeInfo.pos).isInSight(PlayerPacketManager.position.add(0.0, eyeHeight.toDouble(), 0.0), rotation = PlayerPacketManager.rotation) != null
    }

    private enum class SurroundOffset(val offset: BlockPos) {
        DOWN(BlockPos(0, -1, 0)),
        NORTH(BlockPos(0, 0, -1)),
        EAST(BlockPos(1, 0, 0)),
        SOUTH(BlockPos(0, 0, 1)),
        WEST(BlockPos(-1, 0, 0))
    }
}
