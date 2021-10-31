package cum.xiaro.trollhack.gui.hudgui

import cum.xiaro.trollhack.util.interfaces.Nameable
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.safeParallelListener
import cum.xiaro.trollhack.setting.configs.AbstractConfig
import cum.xiaro.trollhack.util.graphics.font.TextComponent
import cum.xiaro.trollhack.util.math.vector.Vec2d

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
