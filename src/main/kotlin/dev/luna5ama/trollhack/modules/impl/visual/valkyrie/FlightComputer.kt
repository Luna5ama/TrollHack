package dev.luna5ama.trollhack.modules.impl.visual.valkyrie

import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.extension.pitch
import dev.luna5ama.trollhack.utils.extension.prevPitch
import dev.luna5ama.trollhack.utils.extension.state
import dev.luna5ama.trollhack.utils.extension.yaw
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.math.vectors.VectorUtils.minus
import dev.luna5ama.trollhack.utils.math.vectors.VectorUtils.plus
import dev.luna5ama.trollhack.utils.math.vectors.VectorUtils.times
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import kotlin.math.*

class FlightComputer : AlwaysListening {
    private var previousRollAngle = 0.0f
    private var currentVelocity = Vec3.ZERO
    private var currentVehicleVelocity = Vec3.ZERO
    private var lastVelocity = Vec3.ZERO
    private var lastVehicleVelocity = Vec3.ZERO

    init {
        nonNullHandler<TickEvent.Post> {
            lastVelocity = currentVelocity
            currentVelocity = player.deltaMovement
            if (player.isPassenger) {
                val entity = player.vehicle!!
                lastVehicleVelocity = currentVehicleVelocity
                currentVehicleVelocity = entity.deltaMovement
            }
        }
    }

    var velocity = Vec3.ZERO!!
    var speed = 0f
    var pitch = 0f
    var heading = 0f
    var flightPitch = 0f
    var flightHeading = 0f
    var roll = 0f
    var altitude = 0f
    var groundLevel = 0
    var distanceFromGround = 0.0f
    var elytraHealth = 0.0f

    context(ctx: NonNullContext)
    fun update(partial: Float): Unit = ctx.run {
        velocity = interpolate(lastVelocity, currentVelocity, partial)
        pitch = computePitch(partial)
        speed = computeSpeed(partial)
        roll = computeRoll(partial)
        heading = computeHeading()
        altitude = computeAltitude()
        groundLevel = computeGroundLevel()
        distanceFromGround = computeDistanceFromGround(altitude, groundLevel)
        flightPitch = computeFlightPitch(velocity, pitch)
        flightHeading = computeFlightHeading(velocity, heading)
        elytraHealth = computeElytraHealth()
    }

    context(ctx: NonNullContext)
    private fun computeElytraHealth(): Float = ctx.run {
        val stack = player.getItemBySlot(EquipmentSlot.CHEST)
        if (stack != null && stack.item == Items.ELYTRA) {
            val remain = (stack.maxDamage.toFloat() - stack.damageValue.toFloat()) / stack.maxDamage.toFloat()
            return remain * 100f
        }
        return 0.0f
    }

    private fun computeFlightPitch(velocity: Vec3, pitch: Float): Float {
        if (velocity.length() < 0.01) {
            return pitch
        }
        val n = velocity.normalize()
        return (90 - Math.toDegrees(acos(n.y))).toFloat()
    }

    private fun computeFlightHeading(velocity: Vec3, heading: Float): Float {
        if (velocity.length() < 0.01) {
            return heading
        }
        return toHeading(Math.toDegrees(-atan2(velocity.x, velocity.z)).toFloat())
    }

    private fun interpolate(vec1: Vec2f, vec2: Vec2f, partial: Float): Vec2f {
        return vec1 + (vec2 - vec1) * partial
    }

    private fun interpolate(vec1: Vec3, vec2: Vec3, partial: Float): Vec3 {
        return vec1 + (vec2 - vec1) * partial
    }

    /**
     * Roll logic is from:
     * https://github.com/Jorbon/cool_elytra/blob/main/src/main/java/edu/jorbonism/cool_elytra/mixin/GameRendererMixin.java
     * to enable both mods will sync up when used together.
     */
    context(ctx: NonNullContext)
    private fun computeRoll(partial: Float): Float = ctx.run {
        val wingPower = Valkyrie.rollTurningForce
        val rollSmoothing = Valkyrie.rollSmoothing
        val interpolatedRotation = interpolate(Vec2f(player.xRotO, player.yRotO), Vec2f(player.pitch, player.yaw), partial)
        val facing = Vec3.directionFromRotation(interpolatedRotation.x, interpolatedRotation.y)
        val velocity = interpolate(lastVelocity, currentVelocity, partial)
        val horizontalFacing2 = facing.horizontalDistanceSqr()
        val horizontalSpeed2 = velocity.horizontalDistanceSqr()

        var rollAngle = 0.0f

        if (horizontalFacing2 > 0.0 && horizontalSpeed2 > 0.0) {
            var dot = (velocity.x * facing.x + velocity.z * facing.z) / sqrt(horizontalFacing2 * horizontalSpeed2)
            dot = Mth.clamp(dot, -1.0, 1.0)
            val direction = sign(velocity.x * facing.z - velocity.z * facing.x)
            rollAngle = ((atan(sqrt(horizontalSpeed2) * acos(dot) * wingPower) * direction
                    * 57.29577951308)).toFloat()
        }

        rollAngle = ((1.0 - rollSmoothing) * rollAngle + rollSmoothing * previousRollAngle).toFloat()
        previousRollAngle = rollAngle

        return rollAngle
    }

    context(ctx: NonNullContext)
    private fun computePitch(partial: Float): Float = ctx.run {
        return (player.pitch + (player.pitch - player.prevPitch) * partial) * -1
    }

    context(ctx: NonNullContext)
    private fun isGround(pos: BlockPos): Boolean = ctx.run {
        return !pos.state.isAir
    }

    context(ctx: NonNullContext)
    fun findGround(): BlockPos? = ctx.run {
        var pos = player.blockPosition()
        while (pos.y >= world.minY) {
            pos = pos.below()
            if (isGround(pos)) {
                return pos
            }
        }
        return null
    }

    context(ctx: NonNullContext)
    private fun computeGroundLevel(): Int = ctx.run {
        return findGround()?.y ?: 0
    }

    private fun computeDistanceFromGround(
        altitude: Float,
        groundLevel: Int
    ): Float {
        return max(0.0, (altitude - groundLevel).toDouble()).toFloat()
    }

    context(ctx: NonNullContext)
    private fun computeAltitude(): Float = ctx.run {
        return player.y.toFloat()
    }

    context(ctx: NonNullContext)
    private fun computeHeading(): Float = ctx.run {
        return toHeading(player.yaw)
    }

    context(ctx: NonNullContext)
    private fun computeSpeed(partial: Float): Float = ctx.run {
        var speed = 0f
        speed = if (player.isPassenger) {
            interpolate(lastVehicleVelocity, currentVehicleVelocity, partial).length().toFloat() * TICKS_PER_SECOND
        } else {
            interpolate(lastVelocity, currentVelocity, partial).length().toFloat() * TICKS_PER_SECOND
        }
        return speed
    }

    private fun toHeading(yawDegrees: Float): Float {
        return (yawDegrees + 180) % 360
    }

    companion object {
        private const val TICKS_PER_SECOND = 20f
    }
}
