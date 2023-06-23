package dev.luna5ama.trollhack.graphics.font.renderer

import dev.luna5ama.trollhack.graphics.font.glyph.FontGlyphs

abstract class AbstractFontRenderContext {
    protected abstract val fontRenderer: IFontRenderer

    var color = -1; protected set
    abstract val variant: FontGlyphs

    abstract fun checkFormatCode(text: CharSequence, index: Int, rendering: Boolean): Boolean
}

class FontRenderContext(override val fontRenderer: FontRenderer) : AbstractFontRenderContext() {
    override val variant get() = fontRenderer.regularGlyph

    override fun checkFormatCode(text: CharSequence, index: Int, rendering: Boolean): Boolean {
        if (index > 0 && text[index - 1] == '§') return true

        if (index < text.length - 1 && text[index] == '§') {
            if (rendering) {
                when (text[index + 1]) {
                    '0' -> color = 0x0
                    '1' -> color = 0x1
                    '2' -> color = 0x2
                    '3' -> color = 0x3
                    '4' -> color = 0x4
                    '5' -> color = 0x5
                    '6' -> color = 0x6
                    '7' -> color = 0x7
                    '8' -> color = 0x8
                    '9' -> color = 0x9
                    'a' -> color = 0xA
                    'b' -> color = 0xB
                    'c' -> color = 0xC
                    'd' -> color = 0xD
                    'e' -> color = 0xE
                    'f' -> color = 0xF
                    'r' -> color = -1
                }
            }
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
            if (rendering) {
                when (text[index + 1]) {
                    '0' -> color = 0x0
                    '1' -> color = 0x1
                    '2' -> color = 0x2
                    '3' -> color = 0x3
                    '4' -> color = 0x4
                    '5' -> color = 0x5
                    '6' -> color = 0x6
                    '7' -> color = 0x7
                    '8' -> color = 0x8
                    '9' -> color = 0x9
                    'a' -> color = 0xA
                    'b' -> color = 0xB
                    'c' -> color = 0xC
                    'd' -> color = 0xD
                    'e' -> color = 0xE
                    'f' -> color = 0xF
                    'l' -> variant = fontRenderer.boldGlyph
                    'o' -> variant = fontRenderer.italicGlyph
                    'r' -> {
                        variant = fontRenderer.regularGlyph
                        color = -1
                    }
                }
            } else {
                when (text[index + 1]) {
                    'r' -> variant = fontRenderer.regularGlyph
                    'l' -> variant = fontRenderer.boldGlyph
                    'o' -> variant = fontRenderer.italicGlyph
                }
            }

            return true
        }

        return false
    }
}
