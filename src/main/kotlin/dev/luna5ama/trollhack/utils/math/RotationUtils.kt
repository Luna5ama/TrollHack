package dev.luna5ama.trollhack.utils.math

import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.extension.pitch
import dev.luna5ama.trollhack.utils.extension.yaw
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.math.vectors.Vec3f
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

import net.minecraft.world.phys.Vec3
import kotlin.math.*

object RotationUtils {
    fun calcAbsAngleDiff(a: Float, b: Float): Float {
        return abs(a - b) % 180.0f
    }

    fun calcAngleDiff(a: Float, b: Float): Float {
        val diff = a - b
        return normalizeAngle(diff)
    }

    fun NonNullContext.faceEntityClosest(entity: Entity) {
        val rotation = getRotationToEntityClosest(entity)
        player.setRotation(rotation)
    }

    fun NonNullContext.getRelativeRotation(entity: Entity): Float {
        return getRelativeRotation(entity.eyePosition)
    }

    fun NonNullContext.getRelativeRotation(posTo: Vec3): Float {
        return getRotationDiff(getRotationTo(posTo), Vec2f(player))
    }

    fun getRotationDiff(r1: Vec2f, r2: Vec2f): Float {
        val r1Radians = r1.toRadians()
        val r2Radians = r2.toRadians()
        return acos(
            cos(r1Radians.y) * cos(r2Radians.y) * cos(r1Radians.x - r2Radians.x) + sin(r1Radians.y) * sin(
                r2Radians.y
            )
        ).toDegree()
    }

    fun NonNullContext.getRotationToEntityClosest(entity: Entity): Vec2f {
        val box = entity.boundingBox

        val eyePos = player.eyePosition

        if (player.boundingBox.intersects(box)) {
            return getRotationTo(eyePos, box.center)
        }

        val x = eyePos.x.coerceIn(box.minX, box.maxX)
        val y = eyePos.y.coerceIn(box.minY, box.maxY)
        val z = eyePos.z.coerceIn(box.minZ, box.maxZ)

        val hitVec = Vec3(x, y, z)
        return getRotationTo(eyePos, hitVec)
    }

    fun NonNullContext.getRotationToEntity(entity: Entity): Vec2f {
        return getRotationTo(entity.position())
    }


    fun NonNullContext.getYawTo(posTo: Vec3): Float {
        val vec = posTo.subtract(player.eyePosition)
        return normalizeAngle((atan2(vec.z, vec.x).toDegree() - 90.0).toFloat())
    }

    /**
     * Get rotation from a player position to another position vector
     *
     * @param posTo Calculate rotation to this position vector
     */
    context(ctx: NonNullContext)
    fun getRotationTo(posTo: Vec3): Vec2f = ctx.run {
        return getRotationTo(player.eyePosition, posTo)
    }


    /**
     * Get rotation from a position vector to another position vector
     *
     * @param posFrom Calculate rotation from this position vector
     * @param posTo Calculate rotation to this position vector
     */
    fun getRotationTo(posFrom: Vec3, posTo: Vec3): Vec2f {
        return getRotationFromVec(posTo.subtract(posFrom))
    }

    fun getRotationFromVec(vec: Vec3): Vec2f {
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

    fun Player.setRotation(rotation: Vec2f) {
        this.setYaw(rotation.x)
        this.setPitch(rotation.y)
    }

    fun Player.setYaw(yaw: Float) {
        this.yaw += normalizeAngle(yaw - this.yaw)
    }

    fun Player.setPitch(pitch: Float) {
        this.pitch = (this.pitch + normalizeAngle(pitch - this.pitch)).coerceIn(-90.0f, 90.0f)
    }

    fun Player.legitRotation(rotation: Vec2f): Vec2f {
        return Vec2f(legitYaw(rotation.x), legitPitch(rotation.y))
    }

    fun Player.legitYaw(yaw: Float): Float {
        return this.yaw + normalizeAngle(yaw - this.yaw)
    }

    fun Player.legitPitch(pitch: Float): Float {
        return (this.pitch + normalizeAngle(pitch - this.pitch)).coerceIn(-90.0f, 90.0f)
    }

    /**
     * @param start The start point
     * @param rotation Angles
     * @return The final point of raytrace
     */
    fun traceRotation(start: Vec3, rotation: Vec2f, dist: Double = 50.0): Vec3 {
        return start.add(
            Vec3(
                dist * -sin(rotation.x.toRadian()) * cos(rotation.y.toRadian()),
                dist * -sin(rotation.y.toRadian()),
                dist * cos(rotation.x.toRadian()) * cos(rotation.y.toRadian())
            )
        )
    }

    fun getDirectionVec(rotation: Vec2f): Vec3f {
        return Vec3f(
            -sin(rotation.x.toRadian()) * cos(rotation.y.toRadian()),
            -sin(rotation.y.toRadian()),
            cos(rotation.x.toRadian()) * cos(rotation.y.toRadian())
        )
    }

    val Direction.yaw: Float
        get() = when (this) {
            Direction.NORTH -> -180.0f
            Direction.SOUTH -> 0.0f
            Direction.EAST -> -90.0f
            Direction.WEST -> 90.0f
            else -> 0.0f
        }
}
