package cum.xiaro.trollhack.gui.rgui.component

import cum.xiaro.trollhack.setting.settings.impl.primitive.BooleanSetting
import cum.xiaro.trollhack.util.math.vector.Vec2f

class SettingButton(val setting: BooleanSetting) : BooleanSlider(setting.name, 0.0f, setting.description, setting.visibility) {

    init {
        if (setting.value) value = 1.0f
    }

    override fun onTick() {
        super.onTick()
        value = if (setting.value) 1.0f else 0.0f
    }

    override fun onClick(mousePos: Vec2f, buttonId: Int) {
        super.onClick(mousePos, buttonId)
        setting.value = !setting.value
    }
}