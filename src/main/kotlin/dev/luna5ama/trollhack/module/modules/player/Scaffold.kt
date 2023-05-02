package dev.luna5ama.trollhack.module.modules.player

import dev.fastmc.common.TickTimer
import dev.fastmc.common.fastFloor
import dev.fastmc.common.sort.ObjectInsertionSort
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.AddCollisionBoxEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.events.player.PlayerMoveEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.HotbarManager.spoofHotbar
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.MovementUtils.realSpeed
import dev.luna5ama.trollhack.util.accessor.syncCurrentPlayItem
import dev.luna5ama.trollhack.util.graphics.ESPRenderer
import dev.luna5ama.trollhack.util.graphics.color.ColorRGB
import dev.luna5ama.trollhack.util.inventory.slot.HotbarSlot
import dev.luna5ama.trollhack.util.inventory.slot.firstItem
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.math.vector.Vec2d
import dev.luna5ama.trollhack.util.world.*
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.minecraft.item.ItemBlock
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
    private val assumePlaced by setting("Assume Placed", false)
    private val maxPendingPlace by setting("Max Pending Place", 2, 1..5, 1)
    private val placeTimeout by setting("Place Timeout", 200, 0..10000, 50)
    private val placeDelay by setting("Place Delay", 100, 0..1000, 1)
    private val rotation by setting("Rotation", true)
    private val extendAxis by setting("Extend Axis", 0.02f, 0.0f..0.1f, 0.001f)
    private val extendDiagonal by setting("Extend Diagonal", 0.02f, 0.0f..0.1f, 0.001f)

    private var lastPos: Vec3d? = null
    private var lastSequence: List<PlaceInfo>? = null
    private val placingPos = LongOpenHashSet()
    private val pendingPlace = Long2LongOpenHashMap()

    private val placeTimer = TickTimer()
    private val renderer = ESPRenderer().apply { aFilled = 31; aOutline = 233 }

    val shouldSafeWalk: Boolean
        get() = isEnabled && safeWalk

    init {
        onDisable {
            lastPos = null
            lastSequence = null
            placingPos.clear()
            pendingPlace.clear()
            placeTimer.reset(-69420L)
        }

        safeListener<AddCollisionBoxEvent> {
            if (!assumePlaced) return@safeListener
            if (it.entity != player) return@safeListener
            if (!pendingPlace.containsKey(it.pos.toLong())) return@safeListener

            it.collidingBoxes.add(AxisAlignedBB(it.pos))
        }

        safeListener<WorldEvent.ClientBlockUpdate> {
            pendingPlace.remove(it.pos.toLong())

            if (!placingPos.contains(it.pos.toLong())) return@safeListener
            updateSequence()
        }

        safeListener<PlayerMoveEvent.Post> {
            val currentTime = System.currentTimeMillis()
            pendingPlace.values.removeIf {
                it < currentTime
            }

            if (lastSequence != null && lastPos == player.positionVector) return@safeListener
            updateSequence()
        }

        safeListener<Render3DEvent> {
            renderer.render(false)
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (!rotation) return@safeListener
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

    private fun SafeClientEvent.runPlacing() {
        if (pendingPlace.size >= maxPendingPlace) return
        if (!placeTimer.tick(placeDelay)) return
        lastSequence?.let {
            val slot = getBlockSlot() ?: return
            for (placeInfo in it) {
                if (pendingPlace.containsKey(placeInfo.placedPos.toLong())) continue
                if (rotation && !checkPlaceRotation(placeInfo)) return
                spoofHotbar(slot) {
                    placeBlock(placeInfo)
                }
                pendingPlace.put(placeInfo.placedPos.toLong(), System.currentTimeMillis() + placeTimeout)
                placeTimer.reset()
                break
            }
        }
    }

    private fun SafeClientEvent.getBlockSlot(): HotbarSlot? {
        playerController.syncCurrentPlayItem()
        return player.hotbarSlots.firstItem<ItemBlock, HotbarSlot>()
    }

    private fun SafeClientEvent.updateSequence() {
        lastPos = player.positionVector
        lastSequence = null
        renderer.clear()

        val feetY = player.posY.fastFloor() - 1

        val feetPos = BlockPos.MutableBlockPos()
        val sequence = calcSortedOffsets().asSequence()
            .mapNotNull { getSequence(feetPos.setPos(it.x.fastFloor(), feetY, it.y.fastFloor())) }
            .firstOrNull()

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
        val bX = player.posX.fastFloor() + 0.5
        val bZ = player.posZ.fastFloor() + 0.5
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
}