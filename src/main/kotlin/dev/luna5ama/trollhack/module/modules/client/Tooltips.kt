package dev.luna5ama.trollhack.module.modules.client

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module

internal object Tooltips : Module(
    name = "Tooltips",
    description = "Displays handy module descriptions in the GUI",
    category = Category.CLIENT,
    visible = false,
    enabledByDefault = true
)