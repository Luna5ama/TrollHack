package dev.luna5ama.trollhack.gui.hudgui.window

import dev.luna5ama.trollhack.gui.hudgui.AbstractHudElement
import dev.luna5ama.trollhack.gui.rgui.windows.SettingWindow
import dev.luna5ama.trollhack.setting.settings.AbstractSetting

class HudSettingWindow(
    hudElement: AbstractHudElement,
    posX: Float,
    posY: Float
) : SettingWindow<AbstractHudElement>(hudElement.name, hudElement, posX, posY, SettingGroup.NONE) {

    override fun getSettingList(): List<AbstractSetting<*>> {
        return element.settingList
    }

}