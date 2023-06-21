package dev.luna5ama.trollhack.gui.clickgui.window

import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.gui.rgui.windows.SettingWindow
import dev.luna5ama.trollhack.module.AbstractModule
import dev.luna5ama.trollhack.setting.settings.AbstractSetting

class ModuleSettingWindow(
    screen: IGuiScreen,
    module: AbstractModule,
) : SettingWindow<AbstractModule>(screen, module.name, module, SettingGroup.NONE) {
    override fun getSettingList(): List<AbstractSetting<*>> {
        return element.fullSettingList.filter { it.name != "Enabled" }
    }
}