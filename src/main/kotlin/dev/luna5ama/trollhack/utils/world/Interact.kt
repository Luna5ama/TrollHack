package dev.luna5ama.trollhack.utils.world

import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.common.floorToInt
import dev.fastmc.common.toDegree
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.serverSideItem
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.ChatUtils
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.collections.EnumSet
import dev.luna5ama.trollhack.utils.extension.isInSight
import dev.luna5ama.trollhack.utils.extension.state
import dev.luna5ama.trollhack.utils.inventory.blockBlacklist
import dev.luna5ama.trollhack.utils.math.vectors.Vec3f
import dev.luna5ama.trollhack.utils.math.vectors.VectorUtils.minus
import dev.luna5ama.trollhack.utils.math.vectors.distanceSqTo
import dev.luna5ama.trollhack.utils.math.vectors.toVec3Center
import net.minecraft.client.multiplayer.prediction.PredictiveAction
import net.minecraft.world.level.block.Blocks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Input
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import java.util.*
import kotlin.math.acos

fun NonNullContext.checkPlaceRotation(placeInfo: PlaceInfo): Boolean {
    var eyeHeight = player.getEyeHeight(Pose.STANDING)
    if (!player.isShiftKeyDown) eyeHeight -= 0.08f
    return AABB(placeInfo.pos).isInSight(
        PlayerPacketManager.position.add(
            0.0,
            eyeHeight.toDouble(),
            0.0
        ), rotation = PlayerPacketManager.rotation
    )
}

context (NonNullContext)
private fun sendSequencedPacket(packetCreator: PredictiveAction) {
    world.blockStatePredictionHandler.startPredicting().use { pendingUpdateManager ->
        val i = pendingUpdateManager.currentSequence()
        val packet = packetCreator.predict(i)
        netHandler.send(packet)
    }
}

context(NonNullContext)
fun PlaceInfo.sendPlacePacket(hand: InteractionHand) =
    sendSequencedPacket {
        ServerboundUseItemOnPacket(
            hand,
            BlockHitResult(
                Vec3(
                    hitVecOffset.x.toDouble() + pos.x,
                    hitVecOffset.y.toDouble() + pos.y,
                    hitVecOffset.z.toDouble() + pos.z
                ),
                this.direction, this.pos, false
            ),
            it
        )
    }

private val SIDES = arrayOf(
    Direction.NORTH,
    Direction.SOUTH,
    Direction.EAST,
    Direction.WEST,
    Direction.UP,
    Direction.DOWN
)

fun NonNullContext.getPlacementSequence(
    targetPos: BlockPos,
    maxDepth: Int,
    vararg options: PlacementSearchOption?
) = getPlacementSequence(targetPos, maxDepth, SIDES, *options)


fun NonNullContext.getPlacementSequence(
    targetPos: BlockPos,
    maxDepth: Int,
    sides: Array<Direction>,
    vararg options: PlacementSearchOption?
): List<PlaceInfo>? {
    val queue = ObjectArrayFIFOQueue<SearchData>()
    val visited = LongOpenHashSet()

    for (side in sides) {
        queue.enqueue(SearchData(targetPos.relative(side), side.opposite, targetPos))
    }

    loop@ while (!queue.isEmpty) {
        val data = queue.dequeue()
        if (!world.isInWorldBounds(data.placedPos)) continue@loop
        if (!world.worldBorder.isWithinBounds(data.placedPos)) continue@loop

        for (option in options) {
            if (option == null) continue
            if (!option.run { check(data.pos, data.side, data.placedPos) }) continue@loop
        }

        if (!data.pos.state.canBeReplaced()) {
            return data.toPlacementSequence(player.eyePosition)
        }

        if (data.depth >= maxDepth) continue
        if (!visited.add(data.pos.asLong())) continue

        for (side in sides) {
            val next = data.next(side)
            queue.enqueue(next)
        }
    }

    return null
}

/**
 * Placing block without desync
 */
fun NonNullContext.placeBlock(
    placeInfo: PlaceInfo,
    hand: InteractionHand = InteractionHand.MAIN_HAND
) {
    if (!world.isPlaceable(placeInfo.placedPos)) return

    val sneak = !player.isShiftKeyDown && blockBlacklist.contains(world.getBlockState(placeInfo.pos).block)
    if (sneak) netHandler.send(ServerboundPlayerInputPacket(Input(false, false, false, false, false, true, player.isSprinting)))

    placeInfo.sendPlacePacket(hand)
    player.swing(hand)

    if (sneak) netHandler.send(ServerboundPlayerInputPacket(Input.EMPTY))

    val itemStack = player.serverSideItem
    val block = (itemStack.item as? BlockItem?)?.block ?: return
//    val metaData = itemStack
    val blockState = block.getStateForPlacement(
        BlockPlaceContext(
            player, hand, itemStack,
            BlockHitResult(
                Vec3(
                    placeInfo.hitVecOffset.x.toDouble() + placeInfo.pos.x,
                    placeInfo.hitVecOffset.y.toDouble() + placeInfo.pos.y,
                    placeInfo.hitVecOffset.z.toDouble() + placeInfo.pos.z
                ),
                placeInfo.direction,
                placeInfo.pos,
                false
            )
        )
    )!!
//    val soundType = blockState.soundGroup.placeSound
//    RenderThreadExecutor.execute {
//        world.playSound(
//            player,
//            placeInfo.pos,
//            soundType,
//            SoundCategory.BLOCKS,
//            1.0f,
//            0.4f
//        )
//    }
}

fun getHitVec(pos: BlockPos, facing: Direction): Vec3 {
    val vec = facing.unitVec3i
    return Vec3(vec.x * 0.5 + 0.5 + pos.x, vec.y * 0.5 + 0.5 + pos.y, vec.z * 0.5 + 0.5 + pos.z)
}

fun getHitVecOffset(facing: Direction): Vec3f {
    val vec = facing.unitVec3i
    return Vec3f(vec.x * 0.5f + 0.5f, vec.y * 0.5f + 0.5f, vec.z * 0.5f + 0.5f)
}

fun NonNullContext.getMiningSide(pos: BlockPos): Direction? {
    val eyePos = player.eyePosition

    return getVisibleSides(pos)
        .filter { !world.getBlockState(pos.relative(it)).isCollisionShapeFullBlock(world, pos) }
        .minByOrNull { eyePos.distanceSqTo(getHitVec(pos, it)) }
}

fun NonNullContext.getClosestVisibleSide(pos: BlockPos): Direction? {
    val eyePos = player.eyePosition

    return getVisibleSides(pos)
        .minByOrNull { eyePos.distanceSqTo(getHitVec(pos, it)) }
}

/**
 * Get the "visible" sides related to player's eye position
 */
fun NonNullContext.getVisibleSides(pos: BlockPos, assumeAirAsFullBox: Boolean = false): Set<Direction> {
    val visibleSides = EnumSet<Direction>()

    val eyePos = player.eyePosition
    val blockCenter = pos.toVec3Center()
    val blockState = world.getBlockState(pos)
    val isFullBox = assumeAirAsFullBox && blockState.block == Blocks.AIR || blockState.isCollisionShapeFullBlock(world, pos)

    return visibleSides
        .checkAxis(eyePos.x - blockCenter.x, Direction.WEST, Direction.EAST, !isFullBox)
        .checkAxis(eyePos.y - blockCenter.y, Direction.DOWN, Direction.UP, true)
        .checkAxis(eyePos.z - blockCenter.z, Direction.NORTH, Direction.SOUTH, !isFullBox)
}

private fun EnumSet<Direction>.checkAxis(
    diff: Double,
    negativeSide: Direction,
    positiveSide: Direction,
    bothIfInRange: Boolean
) =
    this.apply {
        when {
            diff < -0.5 -> {
                add(negativeSide)
            }
            diff > 0.5 -> {
                add(positiveSide)
            }
            else -> {
                if (bothIfInRange) {
                    add(negativeSide)
                    add(positiveSide)
                }
            }
        }
    }

private fun Vec3.calcAngleRadian(Vec31: Vec3): Double {
    return acos(this.dot(Vec31) / (this.length() * Vec31.length()))
}

private fun Vector3f.toVec3() = Vec3(x.toDouble(), y.toDouble(), z.toDouble())

fun NonNullContext.isSideVisible(
    eyeX: Double,
    eyeY: Double,
    eyeZ: Double,
    blockPos: BlockPos,
    side: Direction,
    assumeAirAsFullBox: Boolean = true
): Boolean {
    fun isFullBox(): Boolean {
        val blockState = world.getBlockState(blockPos)
        return assumeAirAsFullBox && blockState.block == Blocks.AIR || blockState.isCollisionShapeFullBlock(world, blockPos)
    }

//    ChatUtils.sendMessage((player.eyePos - getHitVec(blockPos, side)).calcAngleRadian(side.unitVector.toVec3()).toDegree())

    if (ClientSettings.placeStrictRotation)
        return !world.fastRayTrace(player.eyePosition, getHitVec(blockPos, side))
                && (player.eyePosition - getHitVec(blockPos, side)).calcAngleRadian(side.unitVec3).toDegree() < ClientSettings.placeMaxAngle

    return when (side) {
        Direction.DOWN -> {
            eyeY <= blockPos.y
        }
        Direction.UP -> {
            eyeY >= blockPos.y + 1
        }
        Direction.NORTH -> {
            val i = eyeZ.floorToInt()
            i < blockPos.z || i == blockPos.z && isFullBox()
        }
        Direction.SOUTH -> {
            val i = eyeZ.floorToInt()
            i > blockPos.z + 1 || i == blockPos.z + 1 && isFullBox()
        }
        Direction.WEST -> {
            val i = eyeX.floorToInt()
            i < blockPos.x || i == blockPos.x && isFullBox()
        }
        Direction.EAST -> {
            val i = eyeX.floorToInt()
            i > blockPos.x + 1 || i == blockPos.x + 1 && isFullBox()
        }
    }
}

fun interface PlacementSearchOption {
    fun NonNullContext.check(from: BlockPos, side: Direction, to: BlockPos): Boolean

    companion object {
        @JvmStatic
        fun range(range: Float) = range(range.toDouble())

        @JvmStatic
        fun range(range: Double): PlacementSearchOption {
            val rangeSq = range * range
            return PlacementSearchOption { from, side, _ ->
                val sideVec = side.unitVec3i
                val hitX = from.x + sideVec.x * 0.5
                val hitY = from.y + sideVec.y * 0.5
                val hitZ = from.z + sideVec.z * 0.5
                player.distanceToSqr(hitX, hitY, hitZ) <= rangeSq
            }
        }

        @JvmField
        val VISIBLE_SIDE = PlacementSearchOption { from, side, _ ->
            isSideVisible(
                player.x,
                player.eyeY,
                player.z,
                from,
                side
            )
        }

        @JvmField
        val ENTITY_COLLISION = PlacementSearchOption { _, _, to ->
            EntityManager.checkNoEntityCollision(to)
        }

        @JvmField
        val ENTITY_COLLISION_IGNORE_SELF = PlacementSearchOption { _, _, to ->
            EntityManager.checkNoEntityCollision(to)
        }
    }
}

private data class SearchData(
    val prev: SearchData?,
    val pos: BlockPos,
    val side: Direction,
    val placedPos: BlockPos,
    val depth: Int
) {
    constructor(pos: BlockPos, side: Direction, placedPos: BlockPos) :
            this(null, pos, side, placedPos, 1)

    fun next(side: Direction): SearchData {
        val newPos = pos.relative(side.opposite)
        val newPlacedPos = pos
        return SearchData(this, newPos, side, newPlacedPos, depth + 1)
    }

    fun toPlaceInfo(src: Vec3): PlaceInfo {
        val hitVecOffset = getHitVecOffset(side)
        val hitVec = getHitVec(pos, side)

        return PlaceInfo(pos, side, src.distanceTo(hitVec), hitVecOffset, hitVec, placedPos)
    }

    fun toPlacementSequence(src: Vec3): List<PlaceInfo> {
        val sequence = FastObjectArrayList<PlaceInfo>()
        var data: SearchData? = this

        while (data != null) {
            sequence.add(data.toPlaceInfo(src))
            data = data.prev
        }

        return sequence
    }
}
