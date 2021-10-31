package cum.xiaro.trollhack.gui.clickgui.window

import cum.xiaro.trollhack.gui.rgui.windows.SettingWindow
import cum.xiaro.trollhack.module.AbstractModule
import cum.xiaro.trollhack.setting.settings.AbstractSetting

class ModuleSettingWindow(
    module: AbstractModule,
    posX: Float,
    posY: Float
) : SettingWindow<AbstractModule>(module.name, module, posX, posY, SettingGroup.NONE) {

    override fun getSettingList(): List<AbstractSetting<*>> {
        return element.fullSettingList.filter { it.name != "Enabled" && it.name != "Clicks" }
    }

}