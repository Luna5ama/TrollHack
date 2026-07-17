package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module

object Filter : Module("Filter", category = Category.RENDER) {
    val color by setting("Base Color", ColorRGBA(70, 70, 150, 50))
    val isLightMapMode get() = isEnabled

    @JvmStatic
    fun lightMapArgb(): Int = (255 shl 24) or (color.r shl 16) or (color.g shl 8) or color.b
}
