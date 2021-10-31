package cum.xiaro.trollhack.gui.hudgui.window

import cum.xiaro.trollhack.gui.hudgui.AbstractHudElement
import cum.xiaro.trollhack.gui.rgui.windows.SettingWindow
import cum.xiaro.trollhack.setting.settings.AbstractSetting

class HudSettingWindow(
    hudElement: AbstractHudElement,
    posX: Float,
    posY: Float
) : SettingWindow<AbstractHudElement>(hudElement.name, hudElement, posX, posY, SettingGroup.NONE) {

    override fun getSettingList(): List<AbstractSetting<*>> {
        return element.settingList
    }

}