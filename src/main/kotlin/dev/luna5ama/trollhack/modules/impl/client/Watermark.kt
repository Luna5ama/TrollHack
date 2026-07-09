package dev.luna5ama.trollhack.modules.impl.client

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.render.Render2DEvent
import dev.luna5ama.trollhack.manager.managers.UnicodeFontManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.graphics.color.ColorRGBA

object Watermark : Module("Watermark", category = Category.CLIENT) {
    private val color1 by setting("Color1", ColorRGBA.WHITE)
    private val color2 by setting("Color2", ColorRGBA.BLACK)

    init {
        nonNullHandler<Render2DEvent> {
            with (it.context) {
                UnicodeFontManager.GENSHIN_18.drawGradientTextWithShadow(
                    "原神", 10f, 10f, arrayOf(color1.awt, color2.awt))
            }
        }
    }
}