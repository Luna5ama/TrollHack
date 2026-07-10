package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import java.awt.Color

object FullBright : Module("Full Bright", "Maxes out the brightness.",category = Category.RENDER) {
     val color by setting("Color", ColorRGBA.WHITE)
     val rgb get() = Color(color.r, color.g, color.b, color.a)
}