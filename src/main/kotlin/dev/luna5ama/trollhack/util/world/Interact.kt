package dev.luna5ama.trollhack.util.world

import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.common.fastFloor
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.manager.managers.HotbarManager.serverSideItem
import dev.luna5ama.trollhack.manager.managers.HotbarManager.spoofHotbar
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager
import dev.luna5ama.trollhack.module.modules.combat.Surround
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.collections.EnumSet
import dev.luna5ama.trollhack.util.extension.sq
import dev.luna5ama.trollhack.util.inventory.slot.HotbarSlot
import dev.luna5ama.trollhack.util.items.blockBlacklist
import dev.luna5ama.trollhack.util.math.isInSight
import dev.luna5ama.trollhack.util.math.vector.Vec3f
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.math.vector.distanceTo
import dev.luna5ama.trollhack.util.math.vector.toVec3dCenter
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.*



fun SafeClientEvent.checkPlaceRotation(placeInfo: PlaceInfo): Boolean {
    var eyeHeight = player.getEyeHeight()
    if (!player.isSneaking) eyeHeight -= 0.08f
    return AxisAlignedBB(placeInfo.pos).isInSight(
        PlayerPacketManager.position.add(
            0.0,
            eyeHeight.toDouble(),
            0.0
        ), rotation = PlayerPacketManager.rotation
    ) != null
}

fun SafeClientEvent.getPlacement(
    targetPos: BlockPos,
    maxDepth: Int = 3,
    sides: Array<EnumFacing> = SIDES,
    vararg options: PlacementSearchOption?
): PlaceInfo?  {
    val queue = ObjectArrayFIFOQueue<SearchData>()
    val visited = LongOpenHashSet()

    for (side in sides) {
        queue.enqueue(SearchData(targetPos, side, targetPos.offset(side)))
    }

    loop@ while (!queue.isEmpty)  {
        val data = queue.dequeue()
        for (option in options) {
            if (option == null) continue
            if (!option.run { check(data.pos, data.side, data.placedPos) }) continue@loop
        }

        if (!world.getBlock(data.pos).isReplaceable(world, data.pos)) {
            return data.toPlaceInfo(player.eyePosition)
        }

        if (data.depth >= maxDepth) continue
        if (!visited.add(data.pos.toLong())) continue

        for (side in sides) {
            val next = data.next(side)
            queue.enqueue(next)
        }
    }

    return null
}

fun SafeClientEvent.getPlacementSequence(
    targetPos: BlockPos,
    vararg options: PlacementSearchOption?
) = getPlacementSequence(targetPos, 3, SIDES, *options)

fun SafeClientEvent.getPlacementSequence(
    targetPos: BlockPos,
    maxDepth: Int,
    vararg options: PlacementSearchOption?
) = getPlacementSequence(targetPos, maxDepth, SIDES, *options)

fun SafeClientEvent.getPlacementSequence(
    targetPos: BlockPos,
    maxDepth: Int = 3,
    sides: Array<EnumFacing> = SIDES,
    vararg options: PlacementSearchOption?
): List<PlaceInfo>?  {
    val queue = ObjectArrayFIFOQueue<SearchData>()
    val visited = LongOpenHashSet()

    for (side in sides) {
        queue.enqueue(SearchData(targetPos.offset(side), side.opposite, targetPos))
    }

    loop@ while (!queue.isEmpty)  {
        val data = queue.dequeue()
        for (option in options) {
            if (option == null) continue
            if (!option.run { check(data.pos, data.side, data.placedPos) }) continue@loop
        }

        if (!world.getBlock(data.pos).isReplaceable(world, data.pos)) {
            return data.toPlacementSequence(player.eyePosition)
        }

        if (data.depth >= maxDepth) continue
        if (!visited.add(data.pos.toLong())) continue

        for (side in sides) {
            val next = data.next(side)
            queue.enqueue(next)
        }
    }

    return null
}

private val SIDES = arrayOf(
    EnumFacing.NORTH,
    EnumFacing.SOUTH,
    EnumFacing.EAST,
    EnumFacing.WEST,
    EnumFacing.UP,
    EnumFacing.DOWN
)

fun interface PlacementSearchOption {
    fun SafeClientEvent.check(from: BlockPos, side: EnumFacing, to: BlockPos): Boolean

    companion object {
        @JvmStatic
        fun range(range: Double): PlacementSearchOption {
            val rangeSq = range * range
            return PlacementSearchOption { from, side, _ ->
                val sideVec = side.directionVec
                val hitX = from.x + sideVec.x * 0.5
                val hitY = from.y + sideVec.y * 0.5
                val hitZ = from.z + sideVec.z * 0.5
                player.distanceSqTo(hitX, hitY, hitZ) <= rangeSq
            }
        }

        @JvmField
        val VISIBLE_SIDE = PlacementSearchOption { from, side, _ ->
            isSideVisible(
                player.posX,
                player.posY + player.eyeHeight,
                player.posZ,
                from,
                side
            )
        }

        @JvmField
        val ENTITY_COLLISION = PlacementSearchOption { _, _, to ->
                world.checkNoEntityCollision(
                    AxisAlignedBB(to),
                    null
                )
            }
    }
}

private data class SearchData(
    val prev: SearchData?,
    val pos: BlockPos,
    val side: EnumFacing,
    val placedPos: BlockPos,
    val depth: Int
) {
    constructor(pos: BlockPos, side: EnumFacing, placedPos: BlockPos) :
        this(null, pos, side, placedPos, 1)

    fun next(side: EnumFacing): SearchData {
        val newPos = pos.offset(side.opposite)
        val newPlacedPos = pos
        return SearchData(this, newPos, side, newPlacedPos, depth + 1)
    }

    fun toPlaceInfo(src: Vec3d): PlaceInfo {
        val hitVecOffset = getHitVecOffset(side)
        val hitVec = getHitVec(pos, side)

        return PlaceInfo(pos, side, src.distanceTo(hitVec), hitVecOffset, hitVec, placedPos)
    }

    fun toPlacementSequence(src: Vec3d): List<PlaceInfo> {
        val sequence = FastObjectArrayList<PlaceInfo>()
        var data: SearchData? = this

        while (data != null) {
            sequence.add(data.toPlaceInfo(src))
            data = data.prev
        }

        return sequence
    }
}

fun SafeClientEvent.getNeighborSequence(
    pos: BlockPos,
    attempts: Int = 3,
    range: Float = 4.25f,
    visibleSideCheck: Boolean = false,
    entityCheck: Boolean = true,
    sides: Array<EnumFacing> = EnumFacing.values()
) =
    getNeighborSequence(
        player.eyePosition,
        pos,
        attempts,
        range,
        visibleSideCheck,
        entityCheck,
        sides,
        ArrayList(),
        pos,
        0
    )


private fun SafeClientEvent.getNeighborSequence(
    eyePos: Vec3d,
    pos: BlockPos,
    attempts: Int,
    range: Float,
    visibleSideCheck: Boolean,
    entityCheck: Boolean,
    sides: Array<EnumFacing>,
    sequence: ArrayList<PlaceInfo>,
    origin: BlockPos,
    lastDist: Int
): List<PlaceInfo>? {
    for (side in sides) {
        checkNeighbor(eyePos, pos, side, range, visibleSideCheck, entityCheck, true, origin, lastDist)?.let {
            sequence.add(it)
            sequence.reverse()
            return sequence
        }
    }

    if (attempts > 1) {
        for (side in sides) {
            val newPos = pos.offset(side)

            val placeInfo =
                checkNeighbor(eyePos, pos, side, range, visibleSideCheck, entityCheck, false, origin, lastDist)
                    ?: continue
            val newSequence = ArrayList(sequence)
            newSequence.add(placeInfo)

            return getNeighborSequence(
                eyePos,
                newPos,
                attempts - 1,
                range,
                visibleSideCheck,
                entityCheck,
                sides,
                newSequence,
                origin,
                lastDist + 1
            )
                ?: continue
        }
    }

    return null
}

fun SafeClientEvent.getNeighbor(
    pos: BlockPos,
    attempts: Int = 3,
    range: Float = 4.25f,
    visibleSideCheck: Boolean = false,
    entityCheck: Boolean = true,
    sides: Array<EnumFacing> = EnumFacing.values()
) =
    getNeighbor(player.eyePosition, pos, attempts, range, visibleSideCheck, entityCheck, sides, pos, 0)

private fun SafeClientEvent.getNeighbor(
    eyePos: Vec3d,
    pos: BlockPos,
    attempts: Int,
    range: Float,
    visibleSideCheck: Boolean,
    entityCheck: Boolean,
    sides: Array<EnumFacing>,
    origin: BlockPos,
    lastDist: Int
): PlaceInfo? {
    for (side in sides) {
        val result = checkNeighbor(eyePos, pos, side, range, visibleSideCheck, entityCheck, true, origin, lastDist)
        if (result != null) return result
    }

    if (attempts > 1) {
        for (side in sides) {
            val newPos = pos.offset(side)
            if (!world.isPlaceable(newPos)) continue

            return getNeighbor(
                eyePos,
                newPos,
                attempts - 1,
                range,
                visibleSideCheck,
                entityCheck,
                sides,
                origin,
                lastDist + 1
            )
                ?: continue
        }
    }

    return null
}

private fun SafeClientEvent.checkNeighbor(
    eyePos: Vec3d,
    pos: BlockPos,
    side: EnumFacing,
    range: Float,
    visibleSideCheck: Boolean,
    entityCheck: Boolean,
    checkReplaceable: Boolean,
    origin: BlockPos,
    lastDist: Int
): PlaceInfo? {
    val offsetPos = pos.offset(side)
    val oppositeSide = side.opposite

    val distToOrigin = (offsetPos.x - origin.x).sq + (offsetPos.y - origin.y).sq + (offsetPos.z - origin.z).sq
    if (distToOrigin <= lastDist.sq) return null

    val hitVec = getHitVec(offsetPos, oppositeSide)
    val dist = eyePos.distanceTo(hitVec)

    if (dist > range) return null
    if (visibleSideCheck && !getVisibleSides(offsetPos, true).contains(oppositeSide)) return null
    if (checkReplaceable && world.getBlockState(offsetPos).isReplaceable) return null
    if (!world.getBlockState(pos).isReplaceable) return null
    if (entityCheck && !world.checkNoEntityCollision(AxisAlignedBB(pos), null)) return null

    val hitVecOffset = getHitVecOffset(oppositeSide)
    return PlaceInfo(offsetPos, oppositeSide, dist, hitVecOffset, hitVec, pos)
}

fun SafeClientEvent.getMiningSide(pos: BlockPos): EnumFacing? {
    val eyePos = player.eyePosition

    return getVisibleSides(pos)
        .filter { !world.getBlockState(pos.offset(it)).isFullBox }
        .minByOrNull { eyePos.squareDistanceTo(getHitVec(pos, it)) }
}

fun SafeClientEvent.getClosestVisibleSide(pos: BlockPos): EnumFacing? {
    val eyePos = player.eyePosition

    return getVisibleSides(pos)
        .minByOrNull { eyePos.squareDistanceTo(getHitVec(pos, it)) }
}

/**
 * Get the "visible" sides related to player's eye position
 */
fun SafeClientEvent.getVisibleSides(pos: BlockPos, assumeAirAsFullBox: Boolean = false): Set<EnumFacing> {
    val visibleSides = EnumSet<EnumFacing>()

    val eyePos = player.eyePosition
    val blockCenter = pos.toVec3dCenter()
    val blockState = world.getBlockState(pos)
    val isFullBox = assumeAirAsFullBox && blockState.block == Blocks.AIR || blockState.isFullBox

    return visibleSides
        .checkAxis(eyePos.x - blockCenter.x, EnumFacing.WEST, EnumFacing.EAST, !isFullBox)
        .checkAxis(eyePos.y - blockCenter.y, EnumFacing.DOWN, EnumFacing.UP, true)
        .checkAxis(eyePos.z - blockCenter.z, EnumFacing.NORTH, EnumFacing.SOUTH, !isFullBox)
}

private fun EnumSet<EnumFacing>.checkAxis(
    diff: Double,
    negativeSide: EnumFacing,
    positiveSide: EnumFacing,
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

fun getHitVec(pos: BlockPos, facing: EnumFacing): Vec3d {
    val vec = facing.directionVec
    return Vec3d(vec.x * 0.5 + 0.5 + pos.x, vec.y * 0.5 + 0.5 + pos.y, vec.z * 0.5 + 0.5 + pos.z)
}

fun getHitVecOffset(facing: EnumFacing): Vec3f {
    val vec = facing.directionVec
    return Vec3f(vec.x * 0.5f + 0.5f, vec.y * 0.5f + 0.5f, vec.z * 0.5f + 0.5f)
}

/**
 * Placing block without desync
 */
fun SafeClientEvent.placeBlock(
    placeInfo: PlaceInfo,
    hand: EnumHand = EnumHand.MAIN_HAND
) {
    if (!world.isPlaceable(placeInfo.placedPos)) return

    val sneak = !player.isSneaking && blockBlacklist.contains(world.getBlock(placeInfo.pos))
    if (sneak) connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))

    connection.sendPacket(placeInfo.toPlacePacket(hand))
    player.swingArm(hand)

    if (sneak) connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))

    val itemStack = player.serverSideItem
    val block = (itemStack.item as? ItemBlock?)?.block ?: return
    val metaData = itemStack.metadata
    val blockState = block.getStateForPlacement(
        world,
        placeInfo.pos,
        placeInfo.side,
        placeInfo.hitVecOffset.x,
        placeInfo.hitVecOffset.y,
        placeInfo.hitVecOffset.z,
        metaData,
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

fun SafeClientEvent.isSideVisible(
    eyeX: Double,
    eyeY: Double,
    eyeZ: Double,
    blockPos: BlockPos,
    side: EnumFacing,
    assumeAirAsFullBox: Boolean = true
): Boolean {
    fun isFullBox(): Boolean {
        val blockState = world.getBlockState(blockPos)
        return assumeAirAsFullBox && blockState.block == Blocks.AIR || blockState.isFullBox
    }

    return when (side) {
        EnumFacing.DOWN -> {
            eyeY <= blockPos.y
        }
        EnumFacing.UP -> {
            eyeY >= blockPos.y + 1
        }
        EnumFacing.NORTH -> {
            val i = eyeZ.fastFloor()
            i < blockPos.z || i == blockPos.z && isFullBox()
        }
        EnumFacing.SOUTH -> {
            val i = eyeZ.fastFloor()
            i > blockPos.z + 1 || i == blockPos.z + 1 && isFullBox()
        }
        EnumFacing.WEST -> {
            val i = eyeX.fastFloor()
            i < blockPos.x || i == blockPos.x && isFullBox()
        }
        EnumFacing.EAST -> {
            val i = eyeX.fastFloor()
            i > blockPos.x + 1 || i == blockPos.x + 1 && isFullBox()
        }
    }
}

/**
 * Placing block without desync
 */
fun SafeClientEvent.placeBlock(
    placeInfo: PlaceInfo,
    slot: HotbarSlot
) {
    if (!world.isPlaceable(placeInfo.placedPos)) return

    val sneak = !player.isSneaking && blockBlacklist.contains(world.getBlock(placeInfo.pos))
    if (sneak) connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))
    val packet = placeInfo.toPlacePacket(EnumHand.MAIN_HAND)

    spoofHotbar(slot) {
        connection.sendPacket(packet)
    }
    player.swingArm(EnumHand.MAIN_HAND)

    if (sneak) connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))

    val itemStack = player.serverSideItem
    val block = (itemStack.item as? ItemBlock?)?.block ?: return
    val metaData = itemStack.metadata
    val blockState = block.getStateForPlacement(
        world,
        placeInfo.pos,
        placeInfo.side,
        placeInfo.hitVecOffset.x,
        placeInfo.hitVecOffset.y,
        placeInfo.hitVecOffset.z,
        metaData,
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

fun PlaceInfo.toPlacePacket(hand: EnumHand) =
    CPacketPlayerTryUseItemOnBlock(this.pos, this.side, hand, hitVecOffset.x, hitVecOffset.y, hitVecOffset.z)
