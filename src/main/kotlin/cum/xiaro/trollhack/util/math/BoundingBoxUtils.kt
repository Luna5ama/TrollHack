package cum.xiaro.trollhack.util.math

import cum.xiaro.trollhack.util.Wrapper
import cum.xiaro.trollhack.util.math.VectorUtils.plus
import cum.xiaro.trollhack.util.math.VectorUtils.times
import cum.xiaro.trollhack.util.math.VectorUtils.toViewVec
import cum.xiaro.trollhack.util.math.vector.Vec2f
import cum.xiaro.trollhack.util.math.vector.toVec3d
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import kotlin.math.min

inline val AxisAlignedBB.xCenter get() = minX + xLength * 0.5

inline val AxisAlignedBB.yCenter get() = minY + yLength * 0.5

inline val AxisAlignedBB.zCenter get() = minZ + zLength * 0.5

inline val AxisAlignedBB.xLength get() = maxX - minX

inline val AxisAlignedBB.yLength get() = maxY - minY

inline val AxisAlignedBB.zLength get() = maxY - minY

inline val AxisAlignedBB.lengths get() = Vec3d(xLength, yLength, zLength)

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

/**
 * Check if a box is in sight
 */
fun AxisAlignedBB.isInSight(
    posFrom: Vec3d = Wrapper.player?.getPositionEyes(1.0f) ?: Vec3d.ZERO,
    rotation: Vec2f = Wrapper.player?.let { Vec2f(it) } ?: Vec2f.ZERO,
    range: Double = 4.25,
    tolerance: Double = 1.1
) = isInSight(posFrom, rotation.toViewVec(), range, tolerance)

/**
 * Check if a box is in sight
 */
fun AxisAlignedBB.isInSight(
    posFrom: Vec3d,
    viewVec: Vec3d,
    range: Double = 4.25,
    tolerance: Double = 0.1
): RayTraceResult? {
    val sightEnd = posFrom.add(viewVec.scale(range))

    return grow(tolerance).calculateIntercept(posFrom, sightEnd)
}