package dev.luna5ama.trollhack.module.modules.player

import dev.fastmc.common.TickTimer
import dev.fastmc.common.floorToInt
import dev.fastmc.common.sort.ObjectInsertionSort
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.AddCollisionBoxEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.events.player.InputUpdateEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.events.player.PlayerMoveEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.ESPRenderer
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.util.EntityUtils.spoofSneak
import dev.luna5ama.trollhack.util.MovementUtils.realSpeed
import dev.luna5ama.trollhack.util.accessor.syncCurrentPlayItem
import dev.luna5ama.trollhack.util.inventory.slot.HotbarSlot
import dev.luna5ama.trollhack.util.inventory.slot.firstItem
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.math.vector.Vec2d
import dev.luna5ama.trollhack.util.world.*
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

internal object Scaffold : Module(
    name = "Scaffold",
    category = Category.PLAYER,
    description = "Places blocks under you",
    modulePriority = 500
) {
    private val safeWalk by setting("Safe Walk", true)
    private val setbackOnFailure by setting("Setback on Failure", true)
    private val setbackOnFalling by setting("Setback on Falling", true)
    private val assumePlaced by setting("Assume Placed", false)
    private val towerMode by setting("Tower Mode", true)
    private val towerMotion by setting("Tower Jump Motion", 0.42f, 0.0f..1.0f, 0.01f, ::towerMode)
    private val towerJumpHeight by setting("Tower Jump Height Threshold", 0.05f, 0.0f..1.0f, 0.01f, ::towerMode)
    private val towerPlaceHeight by setting("Tower Place Height Threshold", 1.1f, 0.0f..1.5f, 0.01f, ::towerMode)
    private val towerChainLimit by setting("Tower Chain Limit", 10, 1..20, 1, ::towerMode)
    private val towerCooldown by setting("Tower Cooldown", 1, 1..5, 1, ::towerMode)
    private val towerFailureTimeout by setting("Tower Failure Timeout", 500, 0..5000, 1, ::towerMode)
    private val maxPendingPlace by setting("Max Pending Place", 2, 1..5, 1)
    private val placeTimeout by setting("Place Timeout", 200, 0..5000, 50)
    private val placeDelay by setting("Place Delay", 100, 0..1000, 1)
    private val extendAxis by setting("Extend Axis", 0.02f, 0.0f..0.1f, 0.001f)
    private val extendDiagonal by setting("Extend Diagonal", 0.02f, 0.0f..0.1f, 0.001f)

    private val towerFailTimer = TickTimer()
    private var lastJumpHeight = Int.MIN_VALUE
    private var towerCount = -1
    private var lastPos: Vec3d? = null
    private var lastSequence: List<PlaceInfo>? = null
    private val placingPos = LongOpenHashSet()
    private val pendingPlace = Long2LongOpenHashMap().apply {
        defaultReturnValue(-1L)
    }

    private val placeTimer = TickTimer()
    private val renderer = ESPRenderer().apply { aFilled = 31; aOutline = 233 }

    val shouldSafeWalk: Boolean
        get() = isEnabled && safeWalk

    init {
        onDisable {
            resetTower()

            lastPos = null
            lastSequence = null
            placingPos.clear()
            pendingPlace.clear()
            placeTimer.reset(-69420L)
        }

        safeListener<PacketEvent.PostReceive> {
            if (it.packet !is SPacketPlayerPosLook) return@safeListener

            towerFailTimer.reset()
        }

        safeListener<AddCollisionBoxEvent> {
            if (!assumePlaced) return@safeListener
            if (it.entity != player) return@safeListener
            if (!pendingPlace.containsKey(it.pos.toLong())) return@safeListener

            it.collidingBoxes.add(AxisAlignedBB(it.pos))
        }

        safeListener<WorldEvent.ClientBlockUpdate> {
            if (setbackOnFailure
                && pendingPlace.remove(it.pos.toLong()) != -1L && it.newState.block.isReplaceable(world, it.pos)
            ) {
                sendSetbackPacket()
            }

            if (!placingPos.contains(it.pos.toLong())) return@safeListener
            updateSequence()
        }

        safeListener<InputUpdateEvent> {
            if (isTowering()) {
                it.movementInput.moveStrafe = 0.0f
                it.movementInput.moveForward = 0.0f
            }
        }

        safeListener<PlayerMoveEvent.Pre>(-9999) {
            if (!isTowering()) {
                resetTower()
                return@safeListener
            }

            player.motionX = 0.0
            player.motionZ = 0.0

            if (!towerFailTimer.tick(towerFailureTimeout)) {
                resetTower()
                return@safeListener
            }

            val floorY = player.posY.floorToInt()
            if (player.posY - floorY > towerJumpHeight) return@safeListener
            if (floorY == lastJumpHeight) return@safeListener

            lastJumpHeight = floorY
            towerCount++

            if (towerCount <= 0) return@safeListener
            if (towerCount > towerChainLimit) {
                towerCount = -towerCooldown
                return@safeListener
            }

            player.motionY = towerMotion.toDouble()
        }

        safeListener<PlayerMoveEvent.Post> {
            if (setbackOnFalling && player.fallDistance > 3.0f) {
                sendSetbackPacket()
                return@safeListener
            }

            val currentTime = System.currentTimeMillis()
            val prevSize = pendingPlace.size
            pendingPlace.values.removeIf {
                it < currentTime
            }
            if (setbackOnFailure && pendingPlace.size > prevSize) {
                sendSetbackPacket()
                return@safeListener
            }

            updateSequence()
        }

        safeListener<Render3DEvent> {
            renderer.render(false)
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (!Bypass.blockPlaceRotation) return@safeListener
            lastSequence?.let {
                for (placeInfo in it) {
                    if (pendingPlace.containsKey(placeInfo.placedPos.toLong())) continue
                    sendPlayerPacket {
                        rotate(getRotationTo(placeInfo.hitVec))
                    }
                    break
                }
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Post> {
            runPlacing()
        }

        safeListener<RunGameLoopEvent.Tick> {
            runPlacing()
        }
    }

    private fun resetTower() {
        lastJumpHeight = Int.MIN_VALUE
        towerCount = 0
    }

    private fun SafeClientEvent.sendSetbackPacket() {
        connection.sendPacket(CPacketPlayer.Position(player.posX, player.posY + 10.0, player.posZ, false))
    }

    private fun SafeClientEvent.runPlacing() {
        if (pendingPlace.size >= maxPendingPlace) return
        if (!placeTimer.tick(placeDelay)) return

        lastSequence?.let {
            val slot = getBlockSlot() ?: return
            for (placeInfo in it) {
                if (pendingPlace.containsKey(placeInfo.placedPos.toLong())) continue
                if (isTowering() && player.posY - placeInfo.placedPos.y <= towerPlaceHeight) return
                if (Bypass.blockPlaceRotation && !checkPlaceRotation(placeInfo)) return
                player.spoofSneak {
                    ghostSwitch(slot) {
                        placeBlock(placeInfo)
                    }
                }
                pendingPlace.put(placeInfo.placedPos.toLong(), System.currentTimeMillis() + placeTimeout)
                placeTimer.reset()
                break
            }

            renderer.clear()
            for (info in it) {
                val posLong = info.pos.toLong()
                val placedLong = info.placedPos.toLong()
                placingPos.add(posLong)
                placingPos.add(placedLong)

                val box = AxisAlignedBB(info.placedPos)
                if (pendingPlace.containsKey(placedLong)) {
                    renderer.add(box, ColorRGB(64, 255, 64))
                } else {
                    renderer.add(box, ColorRGB(255, 255, 255))
                }
            }
        }
    }

    private fun SafeClientEvent.getBlockSlot(): HotbarSlot? {
        playerController.syncCurrentPlayItem()
        return player.hotbarSlots.firstItem<ItemBlock, HotbarSlot>()
    }

    private val towerSides = arrayOf(
        EnumFacing.DOWN,
        EnumFacing.NORTH,
        EnumFacing.SOUTH,
        EnumFacing.EAST,
        EnumFacing.WEST
    )

    private fun SafeClientEvent.updateSequence() {
        lastPos = player.positionVector
        lastSequence = null
        renderer.clear()

        val floorY = player.posY.floorToInt()
        val sequence = if (isTowering()) {
            val feetY = world.getGroundLevel(player).toInt()
            val feetPos = BlockPos(player.posX.floorToInt(), feetY, player.posZ.floorToInt())
            getPlacementSequence(
                feetPos,
                5,
                towerSides,
                PlacementSearchOption.ENTITY_COLLISION_IGNORE_SELF
            )
        } else {
            val feetY = floorY - 1
            val feetPos = BlockPos.MutableBlockPos()
            calcSortedOffsets().asSequence()
                .mapNotNull { getSequence(feetPos.setPos(it.x.floorToInt(), feetY, it.y.floorToInt())) }
                .firstOrNull()
        }

        lastSequence = sequence

        if (sequence != null) {
            for (info in sequence) {
                val posLong = info.pos.toLong()
                val placedLong = info.placedPos.toLong()
                placingPos.add(posLong)
                placingPos.add(placedLong)

                val box = AxisAlignedBB(info.placedPos)
                if (pendingPlace.containsKey(placedLong)) {
                    renderer.add(box, ColorRGB(64, 255, 64))
                } else {
                    renderer.add(box, ColorRGB(255, 255, 255))
                }
            }
        }
    }

    private fun SafeClientEvent.calcSortedOffsets(): Array<Vec2d> {
        val center = Vec2d(player.posX, player.posZ)
        if (player.realSpeed < 0.05) return arrayOf(center)


        val w = player.width / 2.0
        val axis = w + extendAxis
        val diag = w + extendDiagonal
        val results = arrayOf(
            center,
            center.plus(axis, 0.0),
            center.plus(-axis, 0.0),
            center.plus(0.0, axis),
            center.plus(0.0, -axis),
            center.plus(diag, diag),
            center.plus(diag, -diag),
            center.plus(-diag, diag),
            center.plus(-diag, -diag)
        )
        val bX = player.posX.floorToInt() + 0.5
        val bZ = player.posZ.floorToInt() + 0.5
        val oX = player.posX - bX
        val oZ = player.posZ - bZ
        ObjectInsertionSort.sort(results, 1, results.size, compareByDescending {
            oX * (it.x - bX) + oZ * (it.y - bZ)
        })
        return results
    }

    private fun SafeClientEvent.getSequence(feetPos: BlockPos): List<PlaceInfo>? {
        if (!world.getBlockState(feetPos).isReplaceable) {
            return null
        }
        return getPlacementSequence(
            feetPos,
            5,
            PlacementSearchOption.ENTITY_COLLISION
        )
    }

    private fun SafeClientEvent.isTowering(): Boolean {
        return towerMode && player.movementInput.jump
    }
}