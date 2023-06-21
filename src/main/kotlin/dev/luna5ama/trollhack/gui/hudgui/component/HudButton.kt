package dev.luna5ama.trollhack.gui.hudgui.component

import dev.luna5ama.trollhack.gui.hudgui.AbstractHudElement
import dev.luna5ama.trollhack.gui.hudgui.TrollHudGui
import dev.luna5ama.trollhack.gui.hudgui.window.HudSettingWindow
import dev.luna5ama.trollhack.gui.rgui.component.Button

class HudButton(
    override val screen: TrollHudGui,
    val hudElement: AbstractHudElement
) : Button(
    screen,
    hudElement.name,
    hudElement.description
) {
    override val progress: Float
        get() = if (hudElement.visible) 1.0f else 0.0f

    private val settingWindow by lazy { HudSettingWindow(screen, hudElement) }

    init {
        action { _, buttonId ->
            when (buttonId) {
                0 -> hudElement.visible = !hudElement.visible
                1 -> screen.displayWindow(settingWindow)
            }
        }
    }
}