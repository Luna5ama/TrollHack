package me.luna.trollhack.module.modules.player

import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module

internal object NoRotate : Module(
    name = "NoRotate",
    alias = arrayOf("AntiForceLook"),
    category = Category.PLAYER,
    description = "Stops server packets from turning your head"
)