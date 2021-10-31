package cum.xiaro.trollhack.module.modules.client

import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module

internal object CommandSetting : Module(
    name = "CommandSetting",
    category = Category.CLIENT,
    description = "Settings for commands",
    visible = false,
    alwaysEnabled = true
) {
    var prefix by setting("Prefix", ";")
}