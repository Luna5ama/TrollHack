package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module

internal object NoRotate : Module(
    name = "NoRotate",
    alias = arrayOf("AntiForceLook"),
    category = Category.PLAYER,
    description = "Stops server packets from turning your head"
)