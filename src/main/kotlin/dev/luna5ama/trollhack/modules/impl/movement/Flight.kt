package dev.luna5ama.trollhack.modules.impl.movement

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.UpdateEvent
import dev.luna5ama.trollhack.event.impl.player.AirStrafingSpeedEvent
import dev.luna5ama.trollhack.event.impl.player.IsPlayerInWaterEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.NonNullContext
import net.minecraft.world.phys.Vec3
import kotlin.math.min

object Flight : Module("Flight", category = Category.MOVEMENT) {
    private val horizontalSpeed by setting("Horizontal Speed", 1.0, 0.05..10.0, 0.05)
    private val verticalSpeed by setting("Vertical Speed", 1.0, 0.05..5.0, 0.05)
    private val slowSneaking by setting("Slow Sneaking", true)
    private val antiKick by setting("Anti Kick", false)
    private val antiKickInterval by setting("Anti Kick Interval", 30, 5..80, 1)
    private val antiKickDistance by setting("Anti Kick Distance", 0.07, 0.01..0.2, 0.001)

    private var tickCounter = 0

    init {
        onEnabled {
            tickCounter = 0
        }

        nonNullHandler<UpdateEvent> {
            player.abilities.flying = false
            player.setDeltaMovement(0.0, 0.0, 0.0)

            val velocity = player.deltaMovement
            if (mc.options.keyJump.isDown)
                player.setDeltaMovement(velocity.x, verticalSpeed, velocity.z)
            if (mc.options.keyShift.isDown)
                player.setDeltaMovement(velocity.x, -verticalSpeed, velocity.z)

            if (antiKick) doAntiKick(velocity)
        }

        nonNullHandler<AirStrafingSpeedEvent> { event ->
            var speed = horizontalSpeed.toFloat()
            if (mc.options.keyShift.isDown && slowSneaking)
                speed = min(speed.toDouble(), 0.85).toFloat()
            event.speed = speed
        }

        nonNullHandler<IsPlayerInWaterEvent> { it.inWater = false }
    }

    context (NonNullContext)
    private fun doAntiKick(velocity: Vec3) {
        if (tickCounter > antiKickInterval + 1) tickCounter = 0

        when (tickCounter) {
            0 -> {
                if (mc.options.keyShift.isDown) tickCounter = 2
                else player.setDeltaMovement(
                    velocity.x,
                    -antiKickDistance, velocity.z
                )
            }

            1 -> player.setDeltaMovement(
                velocity.x,
                antiKickDistance, velocity.z
            )
        }
        tickCounter++
    }
}