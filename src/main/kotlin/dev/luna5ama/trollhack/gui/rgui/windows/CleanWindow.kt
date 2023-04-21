package dev.luna5ama.trollhack.gui.rgui.windows

import dev.luna5ama.trollhack.gui.rgui.WindowComponent
import dev.luna5ama.trollhack.setting.GuiConfig
import dev.luna5ama.trollhack.setting.configs.AbstractConfig
import dev.luna5ama.trollhack.util.interfaces.Nameable

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