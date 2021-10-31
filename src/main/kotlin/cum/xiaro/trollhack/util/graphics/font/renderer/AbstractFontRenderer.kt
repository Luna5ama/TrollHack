package cum.xiaro.trollhack.util.graphics.font.renderer

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.TrollHackMod
import cum.xiaro.trollhack.module.modules.client.CustomFont
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import cum.xiaro.trollhack.util.graphics.MatrixUtils
import cum.xiaro.trollhack.util.graphics.font.RenderString
import cum.xiaro.trollhack.util.graphics.font.Style
import cum.xiaro.trollhack.util.graphics.font.glyph.FontGlyphs
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import java.awt.Font

abstract class AbstractFontRenderer(font: Font, size: Float, private val textureSize: Int) : IFontRenderer {
    abstract val renderContext: AbstractFontRenderContext

    protected open val sizeMultiplier
        get() = 1.0f

    protected open val baselineOffset
        get() = 0.0f

    protected open val charGap
        get() = 0.0f

    protected open val lineSpace
        get() = 1.0f

    protected open val lodBias
        get() = 0.0f

    protected open val shadowDist
        get() = 2.0f

    val regularGlyph = loadFont(font, size, Style.REGULAR)

    private var prevCharGap = Float.NaN
    private var prevLineSpace = Float.NaN
    private var prevShadowDist = Float.NaN

    private val renderStringMap = Object2ObjectOpenHashMap<CharSequence, RenderString>()

    private val cleanTimer = TickTimer()

    protected fun loadFont(font: Font, size: Float, style: Style): FontGlyphs {
        // Load fallback font
        val fallbackFont = try {
            getFallbackFont().deriveFont(style.styleConst, size)
        } catch (e: Exception) {
            TrollHackMod.logger.warn("Failed loading fallback font. Using Sans Serif font", e)
            getSansSerifFont().deriveFont(style.styleConst, size)
        }

        return FontGlyphs(style.ordinal, font.deriveFont(style.styleConst, size), fallbackFont, textureSize)
    }

    override fun drawString(charSequence: CharSequence, posX: Float, posY: Float, color: ColorRGB, scale: Float, drawShadow: Boolean) {
        if (cleanTimer.tickAndReset(1000L)) {
            val current = System.currentTimeMillis()
            renderStringMap.values.removeIf {
                it.tryClean(current)
            }
        }

        if (prevCharGap != charGap || prevLineSpace != lineSpace || prevShadowDist != shadowDist) {
            clearStringCache()
            prevCharGap = charGap
            prevLineSpace = lineSpace
            prevShadowDist = shadowDist
        }

        val string = charSequence.toString()
        val stringCache = renderStringMap.computeIfAbsent(string) {
            RenderString(it).build(this, charGap, lineSpace, shadowDist)
        }

        GlStateUtils.texture2d(true)
        GlStateUtils.blend(true)

        val modelView = MatrixUtils.loadModelViewMatrix().getMatrix()
            .translate(posX, posY, 0.0f)
            .scale(sizeMultiplier * scale, sizeMultiplier * scale, 1.0f)
            .translate(0.0f, baselineOffset, 0.0f)

        stringCache.render(modelView, color, drawShadow, lodBias)
    }

    override fun getHeight(scale: Float): Float {
        return regularGlyph.fontHeight
    }

    override fun getWidth(text: CharSequence, scale: Float): Float {
        var maxLineWidth = 0.0f
        var width = 0.0f
        val context = renderContext

        for ((index, char) in text.withIndex()) {
            if (char == '\n') {
                if (width > maxLineWidth) maxLineWidth = width
                width = 0.0f
            }
            if (context.checkFormatCode(text, index, false)) continue
            width += regularGlyph.getCharInfo(char).width + charGap
        }

        return width * sizeMultiplier * scale
    }

    override fun getWidth(char: Char, scale: Float): Float {
        return (regularGlyph.getCharInfo(char).width + charGap) * sizeMultiplier * scale
    }

    open fun destroy() {
        clearStringCache()
        regularGlyph.destroy()
    }

    fun clearStringCache() {
        renderStringMap.values.forEach {
            it.destroy()
        }
        renderStringMap.clear()
    }

    companion object {
        fun getFallbackFont(): Font {
            return Font(fallbackFonts.firstOrNull { CustomFont.availableFonts.containsKey(it) }, Font.PLAIN, 64)
        }

        fun getSansSerifFont(): Font {
            return Font(Font.SANS_SERIF, Font.PLAIN, 64)
        }

        private val fallbackFonts = arrayOf(
            "microsoft yahei ui", "microsoft yahei",
            "noto sans jp", "noto sans cjk jp", "noto sans cjk jp", "noto sans cjk kr", "noto sans cjk sc", "noto sans cjk tc", // Noto Sans
            "source han sans", "source han sans hc", "source han sans sc", "source han sans tc", "source han sans k", // Source Sans
        )
    }
}