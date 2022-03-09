package me.luna.trollhack.gui.hudgui

import me.luna.trollhack.gui.rgui.Component
import me.luna.trollhack.setting.GuiConfig
import me.luna.trollhack.setting.settings.SettingRegister

internal abstract class HudElement(
    name: String,
    alias: Array<String> = emptyArray(),
    category: Category,
    description: String,
    alwaysListening: Boolean = false,
    enabledByDefault: Boolean = false,
) : AbstractHudElement(name, alias, category, description, alwaysListening, enabledByDefault, GuiConfig),
    SettingRegister<Component> by GuiConfig