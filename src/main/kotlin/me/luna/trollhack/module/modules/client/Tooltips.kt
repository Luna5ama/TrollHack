package me.luna.trollhack.module.modules.client

import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module

internal object Tooltips : Module(
    name = "Tooltips",
    description = "Displays handy module descriptions in the GUI",
    category = Category.CLIENT,
    visible = false,
    enabledByDefault = true
)
