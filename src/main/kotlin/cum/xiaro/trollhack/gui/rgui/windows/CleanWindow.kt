package cum.xiaro.trollhack.gui.rgui.windows

import cum.xiaro.trollhack.util.interfaces.Nameable
import cum.xiaro.trollhack.gui.rgui.WindowComponent
import cum.xiaro.trollhack.setting.GuiConfig
import cum.xiaro.trollhack.setting.configs.AbstractConfig

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