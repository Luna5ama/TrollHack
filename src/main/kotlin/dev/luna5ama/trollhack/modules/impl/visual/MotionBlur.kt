package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module

object MotionBlur : Module("Motion Blur", category = Category.RENDER) {
    val strength by setting("Strength", 3.0f)
}