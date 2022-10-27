package me.luna.trollhack.gui.hudgui.component

import me.luna.trollhack.gui.hudgui.AbstractHudElement
import me.luna.trollhack.gui.hudgui.TrollHudGui
import me.luna.trollhack.gui.rgui.component.BooleanSlider
import me.luna.trollhack.util.math.vector.Vec2f

class HudButton(val hudElement: AbstractHudElement) : BooleanSlider(hudElement.name, hudElement.description) {
    override val progress: Float
        get() = if (hudElement.visible) 1.0f else 0.0f

    override fun onClick(mousePos: Vec2f, buttonId: Int) {
        super.onClick(mousePos, buttonId)
        if (buttonId == 0) hudElement.visible = !hudElement.visible
    }

    override fun onRelease(mousePos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, buttonId)
        if (buttonId == 1) TrollHudGui.displaySettingWindow(hudElement)
    }
}