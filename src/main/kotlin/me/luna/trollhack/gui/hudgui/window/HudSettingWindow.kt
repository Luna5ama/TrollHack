package me.luna.trollhack.gui.hudgui.window

import me.luna.trollhack.gui.hudgui.AbstractHudElement
import me.luna.trollhack.gui.rgui.windows.SettingWindow
import me.luna.trollhack.setting.settings.AbstractSetting

class HudSettingWindow(
    hudElement: AbstractHudElement,
    posX: Float,
    posY: Float
) : SettingWindow<AbstractHudElement>(hudElement.name, hudElement, posX, posY, SettingGroup.NONE) {

    override fun getSettingList(): List<AbstractSetting<*>> {
        return element.settingList
    }

}