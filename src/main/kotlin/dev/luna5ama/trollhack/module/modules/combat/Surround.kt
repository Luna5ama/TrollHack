package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.StepEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSetDeadEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.events.player.PlayerMoveEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.HoleManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.module.modules.movement.AutoCenter
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.MovementUtils.isCentered
import dev.luna5ama.trollhack.util.MovementUtils.realSpeed
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.collections.EnumMap
import dev.luna5ama.trollhack.util.combat.CrystalUtils
import dev.luna5ama.trollhack.util.combat.HoleType
import dev.luna5ama.trollhack.util.extension.synchronized
import dev.luna5ama.trollhack.util.inventory.slot.allSlotsPrioritized
import dev.luna5ama.trollhack.util.inventory.slot.firstBlock
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.onMainThreadSafe
import dev.luna5ama.trollhack.util.threads.runSynchronized
import dev.luna5ama.trollhack.util.world.*
import it.unimi.dsi.fastutil.longs.Long2LongMaps
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSets
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.init.Blocks
import net.minecraft.inventory.Slot
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
    private val ghostSwitchBypass by setting("Ghost Switch Bypass", HotbarSwitchManager.Override.DEFAULT)
    private val placeDelay by setting("Place delay", 50, 0..1000, 1)
    private val multiPlace by setting("Multi Place", 2, 1..5, 1)
    private val placeTimeout by setting("Place Timeout", 100, 0..1000, 10)
    private val strictDirection by setting("Strict Direction", false)
    private val autoCenter by setting("Auto Center", true)
    private val autoDisable0 = setting("Auto Disable", AutoDisableMode.OUT_OF_HOLE)
    private val autoDisable by autoDisable0
    private val enableInHole0 = setting("Enable In Hole", false)
    private val enableInHole by enableInHole0
    private val inHoleTimeout by setting(
        "In Hole Timeout",
        50,
        1..100,
        5,
        enableInHole0.atTrue(),
        description = "Delay before enabling Surround when you are in hole, in ticks"
    )

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
            if (event.crystals.none { it.distanceSqTo(player) < 6.0 }) return@safeListener
            var placeCount = 0

            placing.runSynchronized {
                val iterator = values.iterator()
                while (iterator.hasNext()) {
                    val list = iterator.next()
                    var allPlaced = true

                    loop@ for (placeInfo in list) {
                        if (event.crystals.none {
                                CrystalUtils.blockPlaceBoxIntersectsCrystalBox(
                                    placeInfo.placedPos,
                                    it
                                )
                            }) continue

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
            if (Bypass.blockPlaceRotation) {
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
                                rotate(
                                    getRotationTo(
                                        Vec3d(player.posX, player.posY + eyeHeight, player.posZ),
                                        placeInfo.hitVec
                                    )
                                )
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
            val isInHole =
                player.onGround && player.realSpeed < 0.1 && HoleManager.getHoleInfo(playerPos).type == HoleType.OBBY

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
                    true
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

            getPlacementSequence(
                offsetPos,
                2,
                PlacementSearchOption.range(5.0),
                PlacementSearchOption.VISIBLE_SIDE.takeIf { strictDirection },
                PlacementSearchOption { _, _, to -> to != playerPos }
            )?.let { list ->
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

        ghostSwitch(ghostSwitchBypass, slot) {
            connection.sendPacket(placeInfo.toPlacePacket(EnumHand.MAIN_HAND))
        }
        connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))

        if (sneak) connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))

        onMainThreadSafe {
            val blockState = Blocks.OBSIDIAN.getStateForPlacement(
                world,
                placeInfo.pos,
                placeInfo.direction,
                placeInfo.hitVecOffset.x,
                placeInfo.hitVecOffset.y,
                placeInfo.hitVecOffset.z,
                0,
                player,
                EnumHand.MAIN_HAND
            )
            val soundType = blockState.block.getSoundType(blockState, world, placeInfo.pos, player)
            world.playSound(
                player,
                placeInfo.pos,
                soundType.placeSound,
                SoundCategory.BLOCKS,
                (soundType.getVolume() + 1.0f) / 2.0f,
                soundType.getPitch() * 0.8f
            )
        }

        pendingPlacing[placeInfo.placedPos.toLong()] = System.currentTimeMillis() + placeTimeout
    }

    private fun SafeClientEvent.getSlot(): Slot? {
        val slot = player.allSlotsPrioritized.firstBlock(Blocks.OBSIDIAN)

        return if (slot == null) {
            NoSpamMessage.sendMessage("$chatName No obsidian in inventory!")
            null
        } else {
            slot
        }
    }

    private fun SafeClientEvent.checkRotation(placeInfo: PlaceInfo): Boolean {
        return !Bypass.blockPlaceRotation || checkPlaceRotation(placeInfo)
    }

    private enum class SurroundOffset(val offset: BlockPos) {
        DOWN(BlockPos(0, -1, 0)),
        NORTH(BlockPos(0, 0, -1)),
        EAST(BlockPos(1, 0, 0)),
        SOUTH(BlockPos(0, 0, 1)),
        WEST(BlockPos(-1, 0, 0))
    }
}