package me.luna.trollhack.gui.rgui.windows

import me.luna.trollhack.gui.rgui.WindowComponent
import me.luna.trollhack.setting.GuiConfig
import me.luna.trollhack.setting.configs.AbstractConfig
import me.luna.trollhack.util.interfaces.Nameable

/**
 * Window with no rendering
 */
open class CleanWindow(
    name: CharSequence,
    posX: Float,
    posY: Float,
    width: Float,
    height: Float,
    settingGroup: SettingGroup,
    config: AbstractConfig<out Nameable> = GuiConfig
) : WindowComponent(name, posX, posY, width, height, settingGroup, config)