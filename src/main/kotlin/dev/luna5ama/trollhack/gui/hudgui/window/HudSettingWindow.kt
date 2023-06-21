package dev.luna5ama.trollhack.gui.hudgui.window

import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.gui.hudgui.AbstractHudElement
import dev.luna5ama.trollhack.gui.rgui.windows.SettingWindow
import dev.luna5ama.trollhack.setting.settings.AbstractSetting

class HudSettingWindow(
    screen: IGuiScreen,
    hudElement: AbstractHudElement,
) : SettingWindow<AbstractHudElement>(screen, hudElement.name, hudElement, SettingGroup.NONE) {
    override fun getSettingList(): List<AbstractSetting<*>> {
        return element.settingList
    }
}