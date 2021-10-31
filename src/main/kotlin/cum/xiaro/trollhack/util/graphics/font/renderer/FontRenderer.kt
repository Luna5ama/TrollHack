package cum.xiaro.trollhack.util.graphics.font.renderer

import java.awt.Font

open class FontRenderer(font: Font, size: Float, textureSize: Int) : AbstractFontRenderer(font, size, textureSize) {
    override val renderContext: FontRenderContext
        get() = FontRenderContext(this)
}

