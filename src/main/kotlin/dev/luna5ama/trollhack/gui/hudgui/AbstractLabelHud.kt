package dev.luna5ama.trollhack.gui.hudgui

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.setting.configs.AbstractConfig
import dev.luna5ama.trollhack.util.graphics.font.TextComponent
import dev.luna5ama.trollhack.util.interfaces.Nameable
import dev.luna5ama.trollhack.util.math.vector.Vec2d

abstract class AbstractLabelHud(
    name: String,
    alias: Array<String>,
    category: Category,
    description: String,
    alwaysListening: Boolean,
    enabledByDefault: Boolean,
    config: AbstractConfig<out Nameable>,
) : AbstractHudElement(name, alias, category, description, alwaysListening, enabledByDefault, config) {
    override val hudWidth: Float get() = displayText.getWidth() + 2.0f
    override val hudHeight: Float get() = displayText.getHeight(2)

    protected val displayText = TextComponent()

    init {
        safeParallelListener<TickEvent.Post> {
            displayText.clear()
            updateText()
        }
    }

    protected abstract fun SafeClientEvent.updateText()

    override fun renderHud() {
        super.renderHud()

        val textPosX = width * dockingH.multiplier / scale - dockingH.offset
        val textPosY = height * dockingV.multiplier / scale

        displayText.draw(
            Vec2d(textPosX.toDouble(), textPosY.toDouble()),
            horizontalAlign = dockingH,
            verticalAlign = dockingV
        )
    }

}
