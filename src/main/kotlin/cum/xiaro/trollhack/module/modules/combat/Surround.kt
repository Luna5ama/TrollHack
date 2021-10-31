package cum.xiaro.trollhack.module.modules.combat

import cum.xiaro.trollhack.util.collections.EnumMap
import cum.xiaro.trollhack.util.extension.synchronized
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.RunGameLoopEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import cum.xiaro.trollhack.event.events.player.PlayerMoveEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.CombatManager
import cum.xiaro.trollhack.manager.managers.EntityManager
import cum.xiaro.trollhack.manager.managers.HoleManager
import cum.xiaro.trollhack.manager.managers.HotbarManager.spoofHotbar
import cum.xiaro.trollhack.manager.managers.PlayerPacketManager
import cum.xiaro.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.module.modules.movement.AutoCenter
import cum.xiaro.trollhack.util.EntityUtils.betterPosition
import cum.xiaro.trollhack.util.EntityUtils.flooredPosition
import cum.xiaro.trollhack.util.MovementUtils
import cum.xiaro.trollhack.util.MovementUtils.isCentered
import cum.xiaro.trollhack.util.MovementUtils.realSpeed
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.TimeUnit
import cum.xiaro.trollhack.util.atTrue
import cum.xiaro.trollhack.util.combat.HoleType
import cum.xiaro.trollhack.util.inventory.slot.HotbarSlot
import cum.xiaro.trollhack.util.inventory.slot.firstBlock
import cum.xiaro.trollhack.util.inventory.slot.hotbarSlots
import cum.xiaro.trollhack.util.math.RotationUtils.getRotationTo
import cum.xiaro.trollhack.util.math.isInSight
import cum.xiaro.trollhack.util.pause.MainHandPause
import cum.xiaro.trollhack.util.pause.withPause
import cum.xiaro.trollhack.util.text.MessageSendUtils
import cum.xiaro.trollhack.util.threads.onMainThreadSafe
import cum.xiaro.trollhack.util.world.*
import it.unimi.dsi.fastutil.longs.Long2LongMaps
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSets
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.server.SPacketBlockChange
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
    private var inAirTicks = 0
    private var enableTicks = 0

    override fun isActive(): Boolean {
        return isEnabled && placing.isNotEmpty()
    }

    init {
        onDisable {
            placeTimer.reset(-114514L)
            toggleTimer.reset()

            placing.clear()
            placingSet.clear()
            pendingPlacing.clear()
            placed.clear()

            holePos = null
            inAirTicks = 0
            enableTicks = 0
        }

        safeListener<PacketEvent.Receive> { event ->
            if (event.packet is SPacketBlockChange) {
                if (!event.packet.blockState.isReplaceable) {
                    val long = event.packet.blockPosition.toLong()
                    if (placingSet.contains(long)) {
                        pendingPlacing.remove(long)
                        placed.add(long)
                    }
                } else {
                    val pos = event.packet.blockPosition
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
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (rotation) {
                for (list in placing.values) {
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

        safeListener<TickEvent.Pre> {
            inAirTicks++
            enableTicks++
        }

        safeListener<RunGameLoopEvent.Tick>(true) {
            if (!player.onGround) {
                if (isEnabled && inAirTicks >= 20) disable()
                return@safeListener
            } else {
                inAirTicks = 0
            }

            var playerPos = player.betterPosition
            val isInHole = player.onGround && player.realSpeed < 0.1 && HoleManager.getHoleInfo(playerPos).type == HoleType.OBBY

            if (isDisabled) {
                enableInHoleCheck(isInHole)
                return@safeListener
            }

            if (enableTicks > 50 && MovementUtils.isInputting) {
                disable()
            }

            if (world.getBlockState(playerPos).getCollisionBoundingBox(world, playerPos) == null) {
                playerPos = world.getGroundPos(player).up()
            }

            if (isInHole || holePos == null) {
                holePos = playerPos
            }

            // Out of hole check
            if ((!player.onGround || MovementUtils.isInputting) && holePos != playerPos && holePos != player.flooredPosition) {
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
                    if (!player.isCentered(holePos) && (placing.isNotEmpty() || HoleManager.getHoleInfo(holePos).holePos.size != 1)) {
                        AutoCenter.centerPlayer(holePos)
                    }
                }
            }
        }
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
        synchronized(pendingPlacing) {
            pendingPlacing.keys.removeIf {
                if (!world.getBlockState(BlockPos.fromLong(it)).isReplaceable) {
                    placed.add(it)
                } else {
                    false
                }
            }
        }

        if (placing.isEmpty() && (pendingPlacing.isEmpty() || pendingPlacing.values.all { System.currentTimeMillis() > it })) {
            placing.clear()
            placed.clear()
        }

        for (surroundOffset in SurroundOffset.values()) {
            val offsetPos = playerPos.add(surroundOffset.offset)
            if (!world.getBlockState(offsetPos).isReplaceable) continue

            getNeighborSequence(offsetPos, 2, 5.0f, strictDirection)?.let { list ->
                placing[surroundOffset] = list
                list.forEach {
                    placingSet.add(it.placedPos.toLong())
                }
            }
        }
    }

    private fun SafeClientEvent.runPlacing() {
        MainHandPause.withPause(Surround, 50L) {
            var placeCount = 0
            val iterator = placing.values.iterator()

            while (iterator.hasNext()) {
                val list = iterator.next()
                var allPlaced = true

                for (placeInfo in list) {
                    val long = placeInfo.placedPos.toLong()
                    if (placed.contains(long)) continue
                    allPlaced = false

                    if (System.currentTimeMillis() <= pendingPlacing[long]) continue
                    if (!checkRotation(placeInfo)) continue

                    placeBlock(placeInfo)
                    placeCount++
                    if (placeCount >= multiPlace) return
                }

                if (allPlaced) iterator.remove()
            }

            if (autoDisable == AutoDisableMode.ONE_TIME && placing.isEmpty()) {
                disable()
            }
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

        pendingPlacing[placeInfo.placedPos.toLong()] = System.currentTimeMillis() + 50L
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
