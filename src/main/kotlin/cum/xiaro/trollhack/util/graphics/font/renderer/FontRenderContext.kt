package cum.xiaro.trollhack.util.graphics.font.renderer

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.util.graphics.font.glyph.FontGlyphs

abstract class AbstractFontRenderContext {
    protected abstract val fontRenderer: IFontRenderer

    var color = ColorRGB(255, 255, 255); private set
    abstract val variant: FontGlyphs

    abstract fun checkFormatCode(text: CharSequence, index: Int, rendering: Boolean): Boolean

    protected fun checkColorCode(char: Char) {
        when (char) {
            '0' -> color = ColorRGB(0, 0, 0)
            '1' -> color = ColorRGB(0, 0, 170)
            '2' -> color = ColorRGB(0, 170, 0)
            '3' -> color = ColorRGB(0, 170, 170)
            '4' -> color = ColorRGB(170, 0, 0)
            '5' -> color = ColorRGB(170, 0, 170)
            '6' -> color = ColorRGB(250, 170, 0)
            '7' -> color = ColorRGB(170, 170, 170)
            '8' -> color = ColorRGB(85, 85, 85)
            '9' -> color = ColorRGB(85, 85, 255)
            'a' -> color = ColorRGB(85, 255, 85)
            'b' -> color = ColorRGB(85, 255, 255)
            'c' -> color = ColorRGB(255, 85, 85)
            'd' -> color = ColorRGB(255, 85, 255)
            'e' -> color = ColorRGB(255, 255, 85)
            'f' -> color = ColorRGB(255, 255, 255)
            'r' -> color = ColorRGB(255, 255, 255)
        }
    }
}

class FontRenderContext(override val fontRenderer: FontRenderer) : AbstractFontRenderContext() {
    override val variant get() = fontRenderer.regularGlyph

    override fun checkFormatCode(text: CharSequence, index: Int, rendering: Boolean): Boolean {
        if (index > 0 && text[index - 1] == '§') return true

        if (index < text.length - 1 && text[index] == '§') {
            val nextChar = text[index + 1]
            if (rendering) checkColorCode(nextChar)
            return true
        }

        return false
    }
}

class ExtendedFontRenderContext(override val fontRenderer: ExtendedFontRenderer) : AbstractFontRenderContext() {
    override var variant = fontRenderer.regularGlyph; private set

    override fun checkFormatCode(text: CharSequence, index: Int, rendering: Boolean): Boolean {
        if (index > 0 && text[index - 1] == '§') return true

        if (index < text.length - 1 && text[index] == '§') {
            val nextChar = text[index + 1]

            when (nextChar) {
                'r' -> variant = fontRenderer.regularGlyph
                'l' -> variant = fontRenderer.boldGlyph
                'o' -> variant = fontRenderer.italicGlyph
            }

            if (rendering) checkColorCode(nextChar)
            return true
        }

        return false
    }
}