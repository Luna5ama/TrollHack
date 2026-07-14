package dev.luna5ama.trollhack.modules.impl.movement

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.player.InputUpdateEvent
import dev.luna5ama.trollhack.event.impl.player.PlayerUpdateVelocityEvent
import dev.luna5ama.trollhack.manager.managers.RotationManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Input
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max

object MovementFix : Module(
    name = "Movement Fix",
    description = "Keeps movement aligned with your camera while rotating silently.",
    category = Category.MOVEMENT,
    enableByDefault = true
) {
    init {
        nonNullHandler<InputUpdateEvent>(100) {
            if (!MovementFixInputAdapter.shouldApply(RotationManager.isActive, player.isFallFlying)) return@nonNullHandler

            val movementInput = it.movementInput
            movementInput.keyPresses = MovementFixInputAdapter.apply(
                movementInput.keyPresses,
                player.yRot,
                RotationManager.yaw
            )
        }

        nonNullHandler<PlayerUpdateVelocityEvent> {
            if (!MovementFixVelocityAdapter.shouldApply(isEnabled, RotationManager.isActive, player.isFallFlying)) {
                return@nonNullHandler
            }

            it.yaw = RotationManager.yaw
            it.velocity = MovementFixVelocityAdapter.velocity(it.movementInput, it.speed, it.yaw)
            it.cancel()
        }
    }
}

object MovementFixInputAdapter {
    @JvmStatic
    fun apply(input: Input, playerYaw: Float, silentYaw: Float): Input {
        val forward = when {
            input.forward() && !input.backward() -> 1.0f
            input.backward() && !input.forward() -> -1.0f
            else -> 0.0f
        }
        val strafe = when {
            input.left() && !input.right() -> 1.0f
            input.right() && !input.left() -> -1.0f
            else -> 0.0f
        }
        val fixed = MovementFixMath.fix(forward, strafe, playerYaw, silentYaw)
        return Input(
            fixed[0] > 0.0f,
            fixed[0] < 0.0f,
            fixed[1] > 0.0f,
            fixed[1] < 0.0f,
            input.jump(),
            input.shift(),
            input.sprint()
        )
    }

    @JvmStatic
    fun shouldApply(active: Boolean, fallFlying: Boolean): Boolean = active && !fallFlying
}

object MovementFixVelocityAdapter {
    @JvmStatic
    fun shouldApply(enabled: Boolean, active: Boolean, fallFlying: Boolean): Boolean {
        return enabled && active && !fallFlying
    }

    @JvmStatic
    fun velocity(input: Vec3, speed: Float, yaw: Float): Vec3 {
        val lengthSquared = input.lengthSqr()
        if (lengthSquared < 1.0E-7) return Vec3.ZERO

        val scaled = (if (lengthSquared > 1.0) input.normalize() else input).scale(speed.toDouble())
        val radians = yaw * (Math.PI / 180.0)
        val sin = Mth.sin(radians).toDouble()
        val cos = Mth.cos(radians).toDouble()
        return Vec3(
            scaled.x * cos - scaled.z * sin,
            scaled.y,
            scaled.z * cos + scaled.x * sin
        )
    }
}

object MovementFixJumpAdapter {
    @JvmStatic
    fun yaw(
        originalYaw: Float,
        silentYaw: Float,
        localPlayer: Boolean,
        enabled: Boolean,
        active: Boolean,
        fallFlying: Boolean
    ): Float {
        return if (localPlayer && MovementFixVelocityAdapter.shouldApply(enabled, active, fallFlying)) {
            silentYaw
        } else {
            originalYaw
        }
    }
}

object MovementFixMath {
    @JvmStatic
    fun fix(forward: Float, strafe: Float, playerYaw: Float, silentYaw: Float): FloatArray {
        val direction = direction(playerYaw, forward, strafe)
        val directionFactor = max(abs(forward), abs(strafe))
        val angleDifference = wrapDegrees(direction - silentYaw)
        val angleDistance = abs(angleDifference)
        var fixedForward = 0.0f
        var fixedStrafe = 0.0f

        if (angleDistance <= 67.5f) {
            fixedForward++
        } else if (angleDistance >= 112.5f) {
            fixedForward--
        }

        if (angleDifference in 22.5f..157.5f) {
            fixedStrafe--
        } else if (angleDifference in -157.5f..-22.5f) {
            fixedStrafe++
        }

        return floatArrayOf(fixedForward * directionFactor, fixedStrafe * directionFactor)
    }

    private fun direction(yaw: Float, forward: Float, strafe: Float): Float {
        val movingForward = forward > 0.0f
        val movingBack = forward < 0.0f
        val movingPositiveStrafe = strafe > 0.0f
        val movingNegativeStrafe = strafe < 0.0f
        val movingSideways = movingPositiveStrafe || movingNegativeStrafe
        val movingStraight = movingForward || movingBack

        if (movingBack && !movingSideways) return yaw + 180.0f
        if (movingForward && movingNegativeStrafe) return yaw + 45.0f
        if (movingForward && movingPositiveStrafe) return yaw - 45.0f
        if (!movingStraight && movingNegativeStrafe) return yaw + 90.0f
        if (!movingStraight && movingPositiveStrafe) return yaw - 90.0f
        if (movingBack && movingNegativeStrafe) return yaw + 135.0f
        if (movingBack) return yaw - 135.0f
        return yaw
    }

    private fun wrapDegrees(degrees: Float): Float {
        var wrapped = degrees % 360.0f
        if (wrapped >= 180.0f) wrapped -= 360.0f
        if (wrapped < -180.0f) wrapped += 360.0f
        return wrapped
    }
}
