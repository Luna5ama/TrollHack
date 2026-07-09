package dev.luna5ama.trollhack.modules.impl.movement


import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.event.impl.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.extension.isMoving

object Sprint : Module(
    "Sprint",
    "Automatically makes the player sprint",
    Category.MOVEMENT
) {
     private val limit by setting("Limit", true)
    init {
        nonNullHandler<TickEvent.Pre> {
            if (limit) {
                mc.options.keySprint.isDown = true
            }
        }

        nonNullHandler<OnUpdateWalkingPlayerEvent.Pre> {
            if (!limit) {
                if (player.foodData.foodLevel <= 6) return@nonNullHandler
                player.isSprinting = player.isMoving() && !player.isShiftKeyDown
            }
        }
    }
}
