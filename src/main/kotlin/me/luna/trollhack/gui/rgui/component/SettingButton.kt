package me.luna.trollhack.gui.rgui.component

import me.luna.trollhack.setting.settings.impl.primitive.BooleanSetting
import me.luna.trollhack.util.math.vector.Vec2f

class SettingButton(val setting: BooleanSetting) : BooleanSlider(setting.name, setting.description, setting.visibility) {
    override val progress: Float
        get() = if (setting.value) 1.0f else 0.0f

    override fun onClick(mousePos: Vec2f, buttonId: Int) {
        super.onClick(mousePos, buttonId)
        setting.value = !setting.value
    }
}