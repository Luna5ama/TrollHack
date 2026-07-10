package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.render.Skia2DEvent
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module

object RenderTest : Module("Render Test", category = Category.RENDER) {
    private val string by setting("String", "Hello world!")

    init {
        nonNullHandler<Skia2DEvent> {
            it.draw.text(string, 10f, 10f, 14f, ColorRGBA.WHITE, shadow = true)
        }
    }
}
