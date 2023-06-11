package dev.luna5ama.trollhack.util.math

import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.util.math.VectorUtils.plus
import dev.luna5ama.trollhack.util.math.VectorUtils.times
import dev.luna5ama.trollhack.util.math.VectorUtils.toViewVec
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import dev.luna5ama.trollhack.util.math.vector.toVec3d
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.min

val AxisAlignedBB.xCenter get() = minX + xLength * 0.5

val AxisAlignedBB.yCenter get() = minY + yLength * 0.5

val AxisAlignedBB.zCenter get() = minZ + zLength * 0.5

val AxisAlignedBB.xLength get() = maxX - minX

val AxisAlignedBB.yLength get() = maxY - minY

val AxisAlignedBB.zLength get() = maxY - minY

val AxisAlignedBB.lengths get() = Vec3d(xLength, yLength, zLength)

fun AxisAlignedBB.corners(scale: Double): Array<Vec3d> {
    val growSizes = lengths * (scale - 1.0)
    return grow(growSizes.x, growSizes.y, growSizes.z).corners()
}

fun AxisAlignedBB.corners() = arrayOf(
    Vec3d(minX, minY, minZ),
    Vec3d(minX, minY, maxZ),
    Vec3d(minX, maxY, minZ),
    Vec3d(minX, maxY, maxZ),
    Vec3d(maxX, minY, minZ),
    Vec3d(maxX, minY, maxZ),
    Vec3d(maxX, maxY, minZ),
    Vec3d(maxX, maxY, maxZ),
)

fun AxisAlignedBB.side(side: EnumFacing, scale: Double = 0.5): Vec3d {
    val lengths = lengths
    val sideDirectionVec = side.directionVec.toVec3d()
    return lengths * sideDirectionVec * scale + center
}

fun AxisAlignedBB.scale(multiplier: Double): AxisAlignedBB {
    return this.scale(multiplier, multiplier, multiplier)
}

fun AxisAlignedBB.scale(x: Double, y: Double, z: Double): AxisAlignedBB {
    val halfXLength = this.xLength * 0.5
    val halfYLength = this.yLength * 0.5
    val halfZLength = this.zLength * 0.5

    return this.grow(halfXLength * (x - 1.0), halfYLength * (y - 1.0), halfZLength * (z - 1.0))
}

fun AxisAlignedBB.limitSize(x: Double, y: Double, z: Double): AxisAlignedBB {
    val halfX = min(xLength, x) / 2.0
    val halfY = min(yLength, y) / 2.0
    val halfZ = min(zLength, z) / 2.0
    val center = center

    return AxisAlignedBB(
        center.x - halfX, center.y - halfY, center.z - halfZ,
        center.x + halfX, center.y + halfY, center.z + halfZ,
    )
}

fun AxisAlignedBB.intersectsBlock(x: Int, y: Int, z: Int): Boolean {
    return intersects(x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0, y + 1.0, z + 1.0)
}

fun AxisAlignedBB.intersectsBlock(pos: BlockPos): Boolean {
    return intersectsBlock(pos.x, pos.y, pos.z)
}

/**
 * Check if a box is in sight
 */
fun AxisAlignedBB.isInSight(
    posFrom: Vec3d = PlayerPacketManager.position,
    rotation: Vec2f = PlayerPacketManager.rotation,
    range: Double = 8.0
): Boolean {
    return isInSight(posFrom, rotation.toViewVec(), range)
}

/**
 * Check if a box is in sight
 */
fun AxisAlignedBB.isInSight(
    posFrom: Vec3d,
    viewVec: Vec3d,
    range: Double = 4.25
): Boolean {
    val sightEnd = posFrom.add(viewVec.scale(range))
    return grow(Bypass.placeRotationBoundingBoxGrow).intersects(posFrom, sightEnd)
}