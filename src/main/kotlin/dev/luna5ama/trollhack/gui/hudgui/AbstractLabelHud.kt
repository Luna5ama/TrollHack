package dev.luna5ama.trollhack.gui.hudgui

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.graphics.font.TextComponent
import dev.luna5ama.trollhack.setting.configs.AbstractConfig
import dev.luna5ama.trollhack.util.delegate.FrameFloat
import dev.luna5ama.trollhack.util.interfaces.Nameable
import dev.luna5ama.trollhack.util.math.vector.Vec2d
import dev.luna5ama.trollhack.util.text.format
import net.minecraft.util.text.TextFormatting

abstract class AbstractLabelHud(
    name: String,
    alias: Array<String>,
    category: Category,
    description: String,
    alwaysListening: Boolean,
    enabledByDefault: Boolean,
    config: AbstractConfig<out Nameable>,
) : AbstractHudElement(name, alias, category, description, alwaysListening, enabledByDefault, config) {
    override val hudWidth by FrameFloat { displayText.getWidth() + 2.0f }
    override val hudHeight by FrameFloat { displayText.getHeight(2) }

    protected val displayText = TextComponent()

    init {
        safeParallelListener<TickEvent.Post> {
            displayText.clear()
            updateText()
            if (displayText.isEmpty() && screen.isVisible) {
                displayText.add(TextFormatting.ITALIC format nameAsString)
            }
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
