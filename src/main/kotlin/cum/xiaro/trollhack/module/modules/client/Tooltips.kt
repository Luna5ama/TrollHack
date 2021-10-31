package cum.xiaro.trollhack.module.modules.client

import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module

internal object Tooltips : Module(
    name = "Tooltips",
    description = "Displays handy module descriptions in the GUI",
    category = Category.CLIENT,
    visible = false,
    enabledByDefault = true
)
