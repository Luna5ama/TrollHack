package cum.xiaro.trollhack.gui.hudgui.component

import cum.xiaro.trollhack.gui.hudgui.AbstractHudElement
import cum.xiaro.trollhack.gui.hudgui.TrollHudGui
import cum.xiaro.trollhack.gui.rgui.component.BooleanSlider
import cum.xiaro.trollhack.util.math.vector.Vec2f

class HudButton(val hudElement: AbstractHudElement) : BooleanSlider(hudElement.name, 0.0f, hudElement.description) {
    init {
        if (hudElement.visible) value = 1.0f
    }

    override fun onTick() {
        super.onTick()
        value = if (hudElement.visible) 1.0f else 0.0f
    }

    override fun onClick(mousePos: Vec2f, buttonId: Int) {
        super.onClick(mousePos, buttonId)
        if (buttonId == 0) hudElement.visible = !hudElement.visible
    }

    override fun onRelease(mousePos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, buttonId)
        if (buttonId == 1) TrollHudGui.displaySettingWindow(hudElement)
    }
}