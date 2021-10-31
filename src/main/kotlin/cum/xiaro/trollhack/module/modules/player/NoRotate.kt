package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module

internal object NoRotate : Module(
    name = "NoRotate",
    alias = arrayOf("AntiForceLook"),
    category = Category.PLAYER,
    description = "Stops server packets from turning your head"
)