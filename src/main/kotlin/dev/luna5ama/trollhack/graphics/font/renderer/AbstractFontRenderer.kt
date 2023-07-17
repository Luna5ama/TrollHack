package dev.luna5ama.trollhack.graphics.font.renderer

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.graphics.GlStateUtils
import dev.luna5ama.trollhack.graphics.MatrixUtils
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.font.RenderString
import dev.luna5ama.trollhack.graphics.font.Style
import dev.luna5ama.trollhack.graphics.font.glyph.FontGlyphs
import dev.luna5ama.trollhack.module.modules.client.CustomFont
import dev.luna5ama.trollhack.util.extension.synchronized
import dev.luna5ama.trollhack.util.threads.onMainThread
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.joml.Matrix4f
import java.awt.Font

abstract class AbstractFontRenderer(font: Font, size: Float, private val textureSize: Int) : IFontRenderer {
    abstract val renderContext: AbstractFontRenderContext

    protected open val sizeMultiplier
        get() = 1.0f

    protected open val baselineOffset
        get() = 0.0f

    open val charGap
        get() = 0.0f

    protected open val lineSpace
        get() = 1.0f

    protected open val lodBias
        get() = 0.0f

    protected open val shadowDist
        get() = 2.0f

    private val glyphs = ArrayList<FontGlyphs>()
    val regularGlyph = loadFont(font, size, Style.REGULAR)

    private var prevCharGap = Float.NaN
    private var prevLineSpace = Float.NaN
    private var prevShadowDist = Float.NaN

    private val renderStringMap = Object2ObjectOpenHashMap<CharSequence, RenderString>().synchronized()

    private val checkTimer = TickTimer()
    private val cleanTimer = TickTimer()
    private val modelViewMatrix = Matrix4f()

    protected fun loadFont(font: Font, size: Float, style: Style): FontGlyphs {
        // Load fallback font
        val fallbackFont = try {
            getFallbackFont().deriveFont(style.styleConst, size)
        } catch (e: Exception) {
            TrollHackMod.logger.warn("Failed loading fallback font. Using Sans Serif font", e)
            getSansSerifFont().deriveFont(style.styleConst, size)
        }

        return FontGlyphs(style.ordinal, font.deriveFont(style.styleConst, size), fallbackFont, textureSize).also {
            glyphs.add(it)
        }
    }

    override fun drawString(
        charSequence: CharSequence,
        posX: Float,
        posY: Float,
        color: ColorRGB,
        scale: Float,
        drawShadow: Boolean
    ) {
        if (checkTimer.tickAndReset(25L) && glyphs.any { it.checkUpdate() } || cleanTimer.tick(3000L)) {
            val current = System.currentTimeMillis()
            synchronized(renderStringMap) {
                renderStringMap.values.removeIf {
                    it.tryClean(current)
                }
            }
            cleanTimer.reset()
        }

        if (prevCharGap != charGap || prevLineSpace != lineSpace || prevShadowDist != shadowDist) {
            clearStringCache()
            prevCharGap = charGap
            prevLineSpace = lineSpace
            prevShadowDist = shadowDist
        }

        val stringCache = renderStringMap.computeIfAbsent(charSequence.toString()) {
            RenderString(this, it).build(this, charGap, lineSpace, shadowDist)
        }

        GlStateUtils.texture2d(true)
        GlStateUtils.blend(true)

        val modelView = MatrixUtils.loadModelViewMatrix().getMatrix(modelViewMatrix)
            .translate(posX, posY, 0.0f)
            .scale(sizeMultiplier * scale, sizeMultiplier * scale, 1.0f)
            .translate(0.0f, baselineOffset, 0.0f)

        stringCache.render(modelView, color, drawShadow, lodBias)
    }

    override fun getHeight(scale: Float): Float {
        return regularGlyph.fontHeight
    }

    override fun getWidth(text: CharSequence, scale: Float): Float {
        val string = text.toString()
        var renderString = renderStringMap[string]

        if (renderString == null) {
            renderString = RenderString(this, string)

            onMainThread {
                renderStringMap[string] = renderString.build(this, charGap, lineSpace, shadowDist)
            }
        }

        return renderString.width * sizeMultiplier * scale
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
            "microsoft yahei ui",
            "microsoft yahei",
            "noto sans jp",
            "noto sans cjk jp",
            "noto sans cjk jp",
            "noto sans cjk kr",
            "noto sans cjk sc",
            "noto sans cjk tc", // Noto Sans
            "source han sans",
            "source han sans hc",
            "source han sans sc",
            "source han sans tc",
            "source han sans k", // Source Sans
        )
    }
}