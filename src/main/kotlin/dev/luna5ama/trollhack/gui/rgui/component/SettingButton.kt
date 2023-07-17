package dev.luna5ama.trollhack.gui.rgui.component

import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.setting.settings.impl.primitive.BooleanSetting

class SettingButton(
    screen: IGuiScreen,
    val setting: BooleanSetting
) : CheckButton(screen, setting.name, setting.description, setting.visibility) {
    override var state: Boolean by setting
}