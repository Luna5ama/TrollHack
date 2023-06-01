package dev.luna5ama.trollhack.module.modules.render

import dev.luna5ama.trollhack.event.events.InputEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.mixins.core.render.MixinEntityRenderer
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.player.Freecam
import dev.luna5ama.trollhack.util.accessor.unpressKey
import dev.luna5ama.trollhack.util.atTrue

/**
 * @see MixinEntityRenderer
 */
internal object ThirdPersonCamera : Module(
    name = "Third Person Camera",
    category = Category.RENDER,
    description = "Modify 3rd person camera behavior",
    visible = false
) {
    val cameraClip by setting("Camera Clip", true)
    private val whileHolding0 = setting("While Holding", false)
    private val whileHolding by whileHolding0
    private val perspectiveMode by setting("PerspectiveMode Mode", PerspectiveMode.BACK, whileHolding0.atTrue())
    val distance by setting("Camera Distance", 4.0f, 1.0f..10.0f, 0.1f, description = "Camera distance to player")

    @Suppress("unused")
    private enum class PerspectiveMode(val state: Int) {
        BACK(1),
        FRONT(2)
    }

    init {
        listener<InputEvent.Keyboard> {
            if (whileHolding && Freecam.isDisabled && it.key == mc.gameSettings.keyBindTogglePerspective.keyCode) {
                if (it.state) {
                    mc.gameSettings.thirdPersonView = perspectiveMode.state
                } else {
                    mc.gameSettings.thirdPersonView = 0
                }

                mc.gameSettings.keyBindTogglePerspective.unpressKey()
            }
        }
    }
}