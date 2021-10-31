package cum.xiaro.trollhack.util.graphics.font.renderer

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.TrollHackMod
import cum.xiaro.trollhack.module.modules.client.CustomFont
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import cum.xiaro.trollhack.util.graphics.color.ColorUtils
import java.awt.Font

object MainFontRenderer : IFontRenderer {
    private var delegate: ExtendedFontRenderer
    private val defaultFont: Font

    init {
        val inputStream = this.javaClass.getResourceAsStream("/assets/trollhack/fonts/LexendDeca-Regular.ttf")
        defaultFont = Font.createFont(Font.TRUETYPE_FONT, inputStream)

        delegate = loadFont()
    }

    fun reloadFonts() {
        delegate.destroy()
        delegate = loadFont()
    }

    private fun loadFont(): ExtendedFontRenderer {
        val font = try {
            if (CustomFont.isDefaultFont) {
                defaultFont
            } else {
                Font(CustomFont.fontName.value, Font.PLAIN, 64)
            }
        } catch (e: Exception) {
            TrollHackMod.logger.warn("Failed loading main font. Using Sans Serif font.", e)
            AbstractFontRenderer.getSansSerifFont()
        }

        return DelegateFontRenderer(font)
    }

    fun drawStringJava(string: String, posX: Float, posY: Float, color: Int, scale: Float, drawShadow: Boolean) {
        var adjustedColor = color
        if (adjustedColor and -67108864 == 0) adjustedColor = color or -16777216

        GlStateUtils.alpha(false)
        drawString(string, posX, posY - 1.0f, ColorRGB(ColorUtils.argbToRgba(adjustedColor)), scale, drawShadow)
        GlStateUtils.alpha(true)
        GlStateUtils.useProgramForce(0)
    }

    override fun drawString(charSequence: CharSequence, posX: Float, posY: Float, color: ColorRGB, scale: Float, drawShadow: Boolean) {
        delegate.drawString(charSequence, posX, posY, color, scale, drawShadow)
    }

    override fun getWidth(text: CharSequence, scale: Float): Float {
        return delegate.getWidth(text, scale)
    }

    override fun getWidth(char: Char, scale: Float): Float {
        return delegate.getWidth(char, scale)
    }

    override fun getHeight(scale: Float): Float {
        return delegate.run {
            regularGlyph.fontHeight * CustomFont.lineSpace * scale
        }
    }

    private class DelegateFontRenderer(font: Font) : ExtendedFontRenderer(font, 64.0f, 2048) {
        override val sizeMultiplier: Float
            get() = CustomFont.size

        override val baselineOffset: Float
            get() = CustomFont.baselineOffset

        override val charGap: Float
            get() = CustomFont.charGap

        override val lineSpace: Float
            get() = CustomFont.lineSpace

        override val lodBias: Float
            get() = CustomFont.lodBias

        override val shadowDist: Float
            get() = 5.0f
    }
}