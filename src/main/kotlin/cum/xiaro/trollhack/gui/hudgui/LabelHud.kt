package cum.xiaro.trollhack.gui.hudgui

import cum.xiaro.trollhack.gui.rgui.Component
import cum.xiaro.trollhack.setting.GuiConfig
import cum.xiaro.trollhack.setting.settings.SettingRegister

internal abstract class LabelHud(
    name: String,
    alias: Array<String> = emptyArray(),
    category: Category,
    description: String,
    alwaysListening: Boolean = false,
    enabledByDefault: Boolean = false
) : AbstractLabelHud(name, alias, category, description, alwaysListening, enabledByDefault, GuiConfig),
    SettingRegister<Component> by GuiConfig