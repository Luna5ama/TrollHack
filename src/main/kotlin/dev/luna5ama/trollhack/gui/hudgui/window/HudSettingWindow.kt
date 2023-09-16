package dev.luna5ama.trollhack.gui.hudgui.window

import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.gui.hudgui.AbstractHudElement
import dev.luna5ama.trollhack.gui.rgui.windows.SettingWindow
import dev.luna5ama.trollhack.setting.groups.SettingGroup
import dev.luna5ama.trollhack.setting.settings.AbstractSetting

class HudSettingWindow(
    screen: IGuiScreen,
    hudElement: AbstractHudElement,
) : SettingWindow<AbstractHudElement>(screen, hudElement.name, hudElement, UiSettingGroup.NONE) {
    override val elementSettingGroup: SettingGroup
        get() {
            return element.settingGroup
        }

    override val elementSettingList: List<AbstractSetting<*>>
        get() {
            return element.settingList
        }
}