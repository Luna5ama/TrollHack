package dev.luna5ama.trollhack.modules.impl.player

import dev.fastmc.common.floorToInt
import dev.fastmc.common.sort.ObjectInsertionSort
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.LoopEvent
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.event.impl.player.InputUpdateEvent
import dev.luna5ama.trollhack.event.impl.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.impl.player.PlayerMoveEvent
import dev.luna5ama.trollhack.event.impl.render.Render3DEvent
import dev.luna5ama.trollhack.event.impl.world.WorldEvent
import dev.luna5ama.trollhack.manager.managers.EntityMovementManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.compat.clearDirectionalInputCompat
import dev.luna5ama.trollhack.utils.extension.firstItem
import dev.luna5ama.trollhack.utils.extension.realSpeed
import dev.luna5ama.trollhack.graphics.ESPRenderer
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.utils.extension.velocityX
import dev.luna5ama.trollhack.utils.extension.velocityY
import dev.luna5ama.trollhack.utils.extension.velocityZ
import dev.luna5ama.trollhack.utils.extension.yaw
import dev.luna5ama.trollhack.utils.inventory.HotbarSlot
import dev.luna5ama.trollhack.utils.inventory.hotbarSlots
import dev.luna5ama.trollhack.utils.math.RotationUtils
import dev.luna5ama.trollhack.utils.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.utils.math.vectors.Vec2d
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.timing.TickTimer
import dev.luna5ama.trollhack.utils.world.*
import dev.luna5ama.trollhack.utils.world.EntityUtils.spoofSneak
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.item.BlockItem
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object Scaffold : Module(
    name = "Scaffold",
    category = Category.PLAYER,
    description = "Places blocks under you",
    modulePriority = 500
) {
    private val blockPlaceRotation by setting("Rotation", true)
    private val safeWalk by setting("Safe Walk", true)
    private val setbackOnFailure by setting("Setback on Failure", true)
    private val setbackOnFalling by setting("Setback on Falling", true)
    private val grimMode by setting("Grim",false)
    private val towerMode by setting("Tower Mode", true)
    private val towerMotion by setting("Tower Jump Motion", 0.42f, 0.0f..1.0f, 0.01f, { _ -> towerMode })
    private val towerJumpHeight by setting("Tower Jump Height Threshold", 0.05f, 0.0f..1.0f, 0.01f, { _ -> towerMode })
    private val towerPlaceHeight by setting("Tower Place Height Threshold", 1.1f, 0.0f..1.5f, 0.01f, { _ -> towerMode })
    private val towerChainLimit by setting("Tower Chain Limit", 10, 1..20, 1, { _ -> towerMode })
    private val towerCooldown by setting("Tower Cooldown", 1, 1..5, 1, { _ -> towerMode })
    private val towerFailureTimeout by setting("Tower Failure Timeout", 500, 0..5000, 1, { _ -> towerMode })
    private val maxPendingPlace by setting("Max Pending Place", 2, 1..5, 1)
    private val placeTimeout by setting("Place Timeout", 200, 0..5000, 50)
    private val placeDelay by setting("Place Delay", 100, 0..1000, 1)
    private val extendAxis by setting("Extend Axis", 0.02f, 0.0f..0.1f, 0.001f)
    private val extendDiagonal by setting("Extend Diagonal", 0.02f, 0.0f..0.1f, 0.001f)
    private val maxDepth by setting("Max Depth", 5, 0..10)

    private val towerFailTimer = TickTimer()
    private var lastJumpHeight = Int.MIN_VALUE
    private var towerCount = -1
    private var lastPos: Vec3? = null
    private var lastSequence: List<PlaceInfo>? = null
    private val placingPos = LongOpenHashSet()
    private val pendingPlace = Long2LongOpenHashMap().apply {
        defaultReturnValue(-1L)
    }

    private val placeTimer = TickTimer()
    private val renderer = ESPRenderer().apply { aFilled = 31; aOutline = 233 }
    private var lastRotation: Vec2f? = null

    val shouldSafeWalk: Boolean
        get() = isEnabled && safeWalk

    init {
        onDisabled {
            resetTower()
            EntityMovementManager.isSafeWalk = false
            lastPos = null
            lastSequence = null
            lastRotation = null
            placingPos.clear()
            pendingPlace.clear()
            placeTimer.reset(-69420L)
        }

        handler<WorldEvent.Load> {
            disable()
        }

        nonNullHandler<PacketEvent.PostReceive> {
            if (it.packet !is ClientboundPlayerPositionPacket) return@nonNullHandler
            towerFailTimer.reset()
        }

        nonNullHandler<WorldEvent.ClientBlockUpdate> {
            if (setbackOnFailure && pendingPlace.remove(it.pos.asLong()) != -1L && it.newState.canBeReplaced()) {
                sendSetbackPacket()
            }

            if (!placingPos.contains(it.pos.asLong())) return@nonNullHandler
            updateSequence()
        }

        nonNullHandler<InputUpdateEvent> {
            if (isTowering()) {
                it.movementInput.clearDirectionalInputCompat()
            }
        }

        nonNullHandler<PlayerMoveEvent.Pre>(-9999) {
            if (!isTowering()) {
                resetTower()
                return@nonNullHandler
            }

            player.deltaMovement = Vec3(0.0, player.velocityY, 0.0)

            if (!towerFailTimer.tick(towerFailureTimeout)) {
                resetTower()
                return@nonNullHandler
            }

            val floorY = player.y.floorToInt()
            if (player.y - floorY > towerJumpHeight) return@nonNullHandler
            if (floorY == lastJumpHeight) return@nonNullHandler

            lastJumpHeight = floorY
            towerCount++

            if (towerCount <= 0) return@nonNullHandler
            if (towerCount > towerChainLimit) {
                towerCount = -towerCooldown
                return@nonNullHandler
            }

            player.deltaMovement = Vec3(player.velocityX, towerMotion.toDouble(), player.velocityZ)
        }

        nonNullHandler<PlayerMoveEvent.Post> {
            if (setbackOnFalling && player.fallDistance > 3.0f) {
                sendSetbackPacket()
                return@nonNullHandler
            }

            val currentTime = System.currentTimeMillis()
            val prevSize = pendingPlace.size
            pendingPlace.values.removeIf {
                it < currentTime
            }
            if (setbackOnFailure && pendingPlace.size > prevSize) {
                sendSetbackPacket()
                return@nonNullHandler
            }
            updateSequence()
        }

        nonNullHandler<Render3DEvent> {
            renderer.render(false)
        }

        nonNullHandler<OnUpdateWalkingPlayerEvent.Pre> {
            if (!blockPlaceRotation) return@nonNullHandler
            var rotated = false
            lastSequence?.let {
                for (placeInfo in it) {
                    if (isSideVisible(player.eyePosition.x, player.eyePosition.y, player.eyePosition.z, placeInfo.pos, placeInfo.direction)) {
                        if (pendingPlace.containsKey(placeInfo.placedPos.asLong())) continue
                        sendPlayerPacket {
                            val rotation = getRotationTo(placeInfo.hitVec)
                            lastRotation = rotation
                            rotate(rotation)
                        }
                        rotated = true
                        break
                    }
                }
            }
            if (!rotated) {
                val fallbackRotation = lastRotation ?: Vec2f(RotationUtils.normalizeAngle(mc.cameraEntity!!.yaw - 180), 80f)
                sendPlayerPacket {
                    rotate(fallbackRotation)
                }
            }
        }

        nonNullHandler<OnUpdateWalkingPlayerEvent.Post> {
            runPlacing()
            EntityMovementManager.isSafeWalk = shouldSafeWalk
        }

        nonNullHandler<LoopEvent.Tick> {
            runPlacing()
            EntityMovementManager.isSafeWalk = shouldSafeWalk
        }
    }

    private fun resetTower() {
        lastJumpHeight = Int.MIN_VALUE
        towerCount = 0
    }

    private fun NonNullContext.sendSetbackPacket() {
        netHandler.send(ServerboundMovePlayerPacket.Pos(player.x, player.y + 10.0, player.z, false, player.horizontalCollision))
    }

    private fun NonNullContext.runPlacing() {
        if (pendingPlace.size >= maxPendingPlace) return
        if (!placeTimer.tick(placeDelay)) return

        lastSequence?.let {
            val slot = getBlockSlot() ?: return
            for (placeInfo in it) {
                if (pendingPlace.containsKey(placeInfo.placedPos.asLong())) continue
                if (isSideVisible(player.eyePosition.x, player.eyePosition.y, player.eyePosition.z, placeInfo.pos, placeInfo.direction)) {
                    if (isTowering() && player.y - placeInfo.placedPos.y <= towerPlaceHeight) return
                    if (blockPlaceRotation && !checkPlaceRotation(placeInfo)) return
                    player.spoofSneak {
                        ghostSwitch(slot) {
                            placeBlock(placeInfo)
                        }
                    }
                    pendingPlace.put(placeInfo.placedPos.asLong(), System.currentTimeMillis() + placeTimeout)
                    placeTimer.reset()
                    break
                }
            }

            renderer.clear()
            for (info in it) {
                val posLong = info.pos.asLong()
                val placedLong = info.placedPos.asLong()
                placingPos.add(posLong)
                placingPos.add(placedLong)

                val box = AABB(info.placedPos)
                if (pendingPlace.containsKey(placedLong)) {
                    renderer.add(box, ColorRGBA(64, 255, 64))
                } else {
                    renderer.add(box, ColorRGBA(255, 255, 255))
                }
            }
        }
    }

    private fun NonNullContext.getBlockSlot(): HotbarSlot? {
        return player.hotbarSlots.firstItem<BlockItem, HotbarSlot>()
    }

    private val towerSides = arrayOf(
        Direction.DOWN,
        Direction.NORTH,
        Direction.SOUTH,
        Direction.EAST,
        Direction.WEST
    )

    private fun NonNullContext.updateSequence() {
        lastPos = player.position()
        lastSequence = null
        renderer.clear()

        val floorY = player.y.floorToInt()
        val sequence = if (isTowering()) {
            val feetPos = world.getGroundPos(player)
            getPlacementSequence(
                feetPos,
                maxDepth,
                towerSides,
                PlacementSearchOption.ENTITY_COLLISION_IGNORE_SELF,
            )
        } else {
            val feetY = floorY - 1
            val feetPos = BlockPos.MutableBlockPos()
            calcSortedOffsets().asSequence()
                .mapNotNull { getSequence(feetPos.set(it.x.floorToInt(), feetY, it.y.floorToInt())) }
                .firstOrNull()
        }

        lastSequence = sequence

        if (sequence != null) {
            for (info in sequence) {
                val posLong = info.pos.asLong()
                val placedLong = info.placedPos.asLong()
                placingPos.add(posLong)
                placingPos.add(placedLong)

                val box = AABB(info.placedPos)
                if (pendingPlace.containsKey(placedLong)) {
                    renderer.add(box, ColorRGBA(64, 255, 64))
                } else {
                    renderer.add(box, ColorRGBA(255, 255, 255))
                }
            }
        }
    }

    private fun NonNullContext.calcSortedOffsets(): Array<Vec2d> {
        val center = Vec2d(player.x, player.z)
        if (player.realSpeed < 0.05) return arrayOf(center)

        val w = player.bbWidth / 2.0
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
        val bX = player.x.floorToInt() + 0.5
        val bZ = player.z.floorToInt() + 0.5
        val oX = player.x - bX
        val oZ = player.z - bZ
        ObjectInsertionSort.sort(results, 1, results.size, compareByDescending {
            oX * (it.x - bX) + oZ * (it.y - bZ)
        })
        return results
    }

    private fun NonNullContext.getSequence(feetPos: BlockPos): List<PlaceInfo>? {
        if (!world.getBlockState(feetPos).canBeReplaced()) {
            return null
        }

        return getPlacementSequence(
            feetPos,
            maxDepth,
            PlacementSearchOption.ENTITY_COLLISION,
        )
    }

    private fun NonNullContext.isTowering(): Boolean {
        return towerMode && player.input.keyPresses.jump
    }

    context(NonNullContext)
    private fun isOffsetBBEmpty(x: Double, z: Double): Boolean {
        return !world.getBlockCollisions(
            player,
            player.boundingBox.inflate(-0.1, 0.1, -0.1).move(x, -2.0, z)
        ).iterator().hasNext()
    }
}
