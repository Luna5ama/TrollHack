package cum.xiaro.trollhack.util.math

import cum.xiaro.trollhack.util.extension.toDegree
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.util.EntityUtils.eyePosition
import cum.xiaro.trollhack.util.math.vector.Vec2f
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import kotlin.math.*

/**
 * Utils for calculating angles and rotations
 */
object RotationUtils {
    fun calcAbsAngleDiff(a: Float, b: Float): Float {
        return abs(a - b) % 180.0f
    }

    fun calcAngleDiff(a: Float, b: Float): Float {
        val diff = a - b
        return normalizeAngle(diff)
    }

    fun SafeClientEvent.faceEntityClosest(entity: Entity) {
        val rotation = getRotationToEntityClosest(entity)
        player.setRotation(rotation)
    }

    fun SafeClientEvent.getRelativeRotation(entity: Entity): Float {
        return getRelativeRotation(entity.eyePosition)
    }

    fun SafeClientEvent.getRelativeRotation(posTo: Vec3d): Float {
        return getRotationDiff(getRotationTo(posTo), Vec2f(player))
    }

    fun getRotationDiff(r1: Vec2f, r2: Vec2f): Float {
        val r1Radians = r1.toRadians()
        val r2Radians = r2.toRadians()
        return acos(cos(r1Radians.y) * cos(r2Radians.y) * cos(r1Radians.x - r2Radians.x) + sin(r1Radians.y) * sin(r2Radians.y)).toDegree()
    }

    fun SafeClientEvent.getRotationToEntityClosest(entity: Entity): Vec2f {
        val box = entity.entityBoundingBox

        val eyePos = player.eyePosition

        if (player.entityBoundingBox.intersects(box)) {
            return getRotationTo(eyePos, box.center)
        }

        val x = eyePos.x.coerceIn(box.minX, box.maxX)
        val y = eyePos.y.coerceIn(box.minY, box.maxY)
        val z = eyePos.z.coerceIn(box.minZ, box.maxZ)

        val hitVec = Vec3d(x, y, z)
        return getRotationTo(eyePos, hitVec)
    }

    fun SafeClientEvent.getRotationToEntity(entity: Entity): Vec2f {
        return getRotationTo(entity.positionVector)
    }

    /**
     * Get rotation from a player position to another position vector
     *
     * @param posTo Calculate rotation to this position vector
     */
    fun SafeClientEvent.getRotationTo(posTo: Vec3d): Vec2f {
        return getRotationTo(player.getPositionEyes(1f), posTo)
    }

    fun SafeClientEvent.getYawTo(posTo: Vec3d): Float {
        val vec = posTo.subtract(player.eyePosition)
        return normalizeAngle((atan2(vec.z, vec.x).toDegree() - 90.0).toFloat())
    }

    /**
     * Get rotation from a position vector to another position vector
     *
     * @param posFrom Calculate rotation from this position vector
     * @param posTo Calculate rotation to this position vector
     */
    fun getRotationTo(posFrom: Vec3d, posTo: Vec3d): Vec2f {
        return getRotationFromVec(posTo.subtract(posFrom))
    }

    fun getRotationFromVec(vec: Vec3d): Vec2f {
        val xz = hypot(vec.x, vec.z)
        val yaw = normalizeAngle(atan2(vec.z, vec.x).toDegree() - 90.0)
        val pitch = normalizeAngle(-atan2(vec.y, xz).toDegree())
        return Vec2f(yaw, pitch)
    }

    fun normalizeAngle(angleIn: Double): Double {
        var angle = angleIn
        angle %= 360.0
        if (angle >= 180.0) {
            angle -= 360.0
        }
        if (angle < -180.0) {
            angle += 360.0
        }
        return angle
    }

    fun normalizeAngle(angleIn: Float): Float {
        var angle = angleIn
        angle %= 360.0f

        if (angle >= 180.0f) {
            angle -= 360.0f
        } else if (angle < -180.0f) {
            angle += 360.0f
        }

        return angle
    }

    fun EntityPlayerSP.setRotation(rotation: Vec2f) {
        this.setYaw(rotation.x)
        this.setPitch(rotation.y)
    }

    fun EntityPlayerSP.setYaw(yaw: Float) {
        this.rotationYaw += normalizeAngle(yaw - this.rotationYaw)
    }

    fun EntityPlayerSP.setPitch(pitch: Float) {
        this.rotationPitch = (this.rotationPitch + normalizeAngle(pitch - this.rotationPitch)).coerceIn(-90.0f, 90.0f)
    }

    fun EntityPlayerSP.legitRotation(rotation: Vec2f): Vec2f {
        return Vec2f(legitYaw(rotation.x), legitPitch(rotation.y))
    }

    fun EntityPlayerSP.legitYaw(yaw: Float): Float {
        return this.rotationYaw + normalizeAngle(yaw - this.rotationYaw)
    }

    fun EntityPlayerSP.legitPitch(pitch: Float): Float {
        return (this.rotationPitch + normalizeAngle(pitch - this.rotationPitch)).coerceIn(-90.0f, 90.0f)
    }

    val EnumFacing.yaw: Float
        get() = when (this) {
            EnumFacing.NORTH -> -180.0f
            EnumFacing.SOUTH -> 0.0f
            EnumFacing.EAST -> -90.0f
            EnumFacing.WEST -> 90.0f
            else -> 0.0f
        }
}