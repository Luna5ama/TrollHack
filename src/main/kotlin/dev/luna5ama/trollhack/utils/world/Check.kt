package dev.luna5ama.trollhack.utils.world

import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.utils.ImplicitOverriding
import dev.luna5ama.trollhack.utils.MinecraftWrapper
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.math.floorToInt
import dev.luna5ama.trollhack.utils.math.vectors.distanceSqTo
import net.minecraft.world.entity.Entity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.VoxelShape

context(NonNullContext)
fun Level.getGroundPos(entity: Entity): BlockPos {
    return getGroundPos(entity.boundingBox)
}

context(NonNullContext)
fun Level.getGroundPos(boundingBox: AABB): BlockPos {
    val heights = mutableListOf<BlockPos>()
    val pos = BlockPos.MutableBlockPos()

    for (x in (boundingBox.minX - 0.1).floorToInt()..(boundingBox.maxX + 0.1).floorToInt()) {
        for (z in (boundingBox.minZ - 0.1).floorToInt()..(boundingBox.maxZ + 0.1).floorToInt()) {
            if (!getBlockState(BlockPos(x, (boundingBox.minY + 0.5).floorToInt(), z)).canBeReplaced()) continue
            var minYBoundingBox = boundingBox.minY
            for (y in (boundingBox.minY + 0.5).floorToInt() downTo minY - 2) {
                pos.set(x, y, z)
                if (getBlockState(pos).canBeReplaced()) {
                    minYBoundingBox = y.toDouble()
                } else {
                    heights.add(BlockPos(x, minYBoundingBox.floorToInt(), z))
                    break
                }
            }
            heights.add(BlockPos(x, minYBoundingBox.floorToInt(), z))
        }
    }

    return heights.distinct().sortedWith { pos1, pos2 ->
        if (pos1.y == pos2.y) player.distanceSqTo(pos1.x + 0.5, pos1.y.toDouble(), pos1.z + 0.5)
            .compareTo(player.distanceSqTo(pos2.x + 0.5, pos2.y.toDouble(), pos2.z + 0.5))
        else (player.y - pos1.y).compareTo(player.y - pos2.y)
    }.firstOrNull() ?: BlockPos(boundingBox.minX.toInt(), minY, boundingBox.minZ.toInt())
}

fun Level.getCollisionBox(pos: BlockPos): VoxelShape =
    this.getBlockState(pos).getCollisionShape(this, pos)

/**
 * Checks if given [pos] is able to place block in it
 *
 * @return true playing is not colliding with [pos] and there is block below it
 */
@OptIn(ImplicitOverriding::class)
fun Level.isPlaceable(pos: BlockPos, ignoreSelfCollide: Boolean = false) =
    this.getBlockState(pos).canBeReplaced()
            && EntityManager.checkNoEntityCollision(AABB(pos), if (ignoreSelfCollide) MinecraftWrapper.player!! else null)

fun Level.checkBlockCollision(pos: BlockPos, box: AABB, tolerance: Double = 0.005): Boolean {
    val blockBox = getCollisionBox(pos).takeUnless { it.isEmpty }?.bounds() ?: return false
    return box.intersects(
        pos.x + blockBox.minX + tolerance, pos.y + blockBox.minY + tolerance, pos.z + blockBox.minZ + tolerance,
        pos.x + blockBox.maxX - tolerance, pos.y + blockBox.maxY - tolerance, pos.z + blockBox.maxZ - tolerance
    )
}