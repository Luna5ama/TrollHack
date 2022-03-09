package me.luna.trollhack.module.modules.client

import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module

internal object CommandSetting : Module(
    name = "CommandSetting",
    category = Category.CLIENT,
    description = "Settings for commands",
    visible = false,
    alwaysEnabled = true
) {
    var prefix by setting("Prefix", ";")
}