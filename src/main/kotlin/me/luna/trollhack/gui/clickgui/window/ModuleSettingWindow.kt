package me.luna.trollhack.gui.clickgui.window

import me.luna.trollhack.gui.rgui.windows.SettingWindow
import me.luna.trollhack.module.AbstractModule
import me.luna.trollhack.setting.settings.AbstractSetting

class ModuleSettingWindow(
    module: AbstractModule,
    posX: Float,
    posY: Float
) : SettingWindow<AbstractModule>(module.name, module, posX, posY, SettingGroup.NONE) {

    override fun getSettingList(): List<AbstractSetting<*>> {
        return element.fullSettingList.filter { it.name != "Enabled" && it.name != "Clicks" }
    }

}