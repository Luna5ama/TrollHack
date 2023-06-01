package dev.luna5ama.trollhack.module.modules.client

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module

internal object CommandSetting : Module(
    name = "Command Setting",
    category = Category.CLIENT,
    description = "Settings for commands",
    visible = false,
    alwaysEnabled = true
) {
    var prefix by setting("Prefix", ";")
}