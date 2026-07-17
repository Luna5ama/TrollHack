package dev.luna5ama.trollhack.modules.impl.client

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.render.Skia2DEvent
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module

object Watermark : Module("Watermark", category = Category.CLIENT) {
    private val color1 by setting("Color 1", ColorRGBA.WHITE)
    private val color2 by setting("Color 2", ColorRGBA.BLACK)

    init {
        nonNullHandler<Skia2DEvent> {
            it.draw.gradientText("TrollHack", 10f, 10f, 18f, color1, color2, shadow = true)
        }
    }
}
