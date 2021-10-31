package cum.xiaro.trollhack.util.math

import cum.xiaro.trollhack.util.extension.*
import cum.xiaro.trollhack.util.math.vector.Vec2f
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import kotlin.math.cos
import kotlin.math.sin

@Suppress("NOTHING_TO_INLINE")
object VectorUtils {
    /**
     * Get all block positions inside a sphere with given [radius]
     *
     * @param center Center of the sphere
     * @param radius Radius of the sphere
     * @return block positions inside a sphere with given [radius]
     */
    fun getBlockPosInSphere(center: Vec3d, radius: Float): Sequence<BlockPos> {
        val squaredRadius = radius.sq
        val blockPos = BlockPos.MutableBlockPos()

        return sequence {
            for (x in getAxisRange(center.x, radius)) {
                for (y in getAxisRange(center.y, radius)) {
                    for (z in getAxisRange(center.z, radius)) {
                        blockPos.setPos(x, y, z)
                        if (blockPos.distanceSqToCenter(center.x, center.y, center.z) > squaredRadius) continue
                        yield(blockPos.toImmutable())
                    }
                }
            }
        }
    }

    private inline fun getAxisRange(d1: Double, d2: Float): IntRange {
        return IntRange((d1 - d2).fastFloor(), (d1 + d2).fastCeil())
    }

    fun Vec2f.toViewVec(): Vec3d {
        val yawRad = this.x.toDouble().toRadian()
        val pitchRag = this.y.toDouble().toRadian()
        val yaw = -yawRad - PI_FLOAT
        val pitch = -pitchRag

        val cosYaw = cos(yaw)
        val sinYaw = sin(yaw)
        val cosPitch = -cos(pitch)
        val sinPitch = sin(pitch)

        return Vec3d(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch)
    }

    inline fun Vec3i.multiply(multiplier: Int): Vec3i {
        return Vec3i(this.x * multiplier, this.y * multiplier, this.z * multiplier)
    }

    inline infix operator fun Vec3d.times(vec3d: Vec3d): Vec3d = Vec3d(x * vec3d.x, y * vec3d.y, z * vec3d.z)

    inline infix operator fun Vec3d.times(multiplier: Double): Vec3d = Vec3d(x * multiplier, y * multiplier, z * multiplier)

    inline infix operator fun Vec3d.plus(vec3d: Vec3d): Vec3d = add(vec3d)

    inline infix operator fun Vec3d.minus(vec3d: Vec3d): Vec3d = subtract(vec3d)

    inline fun BlockPos.MutableBlockPos.setAndAdd(set: BlockPos, add: BlockPos): BlockPos.MutableBlockPos {
        return this.setPos(set.x + add.x, set.y + add.y, set.z + add.z)
    }

    inline fun BlockPos.MutableBlockPos.setAndAdd(set: BlockPos, x: Int, y: Int, z: Int): BlockPos.MutableBlockPos {
        return this.setPos(set.x + x, set.y + y, set.z + z)
    }
}

