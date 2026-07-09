package dev.luna5ama.trollhack.graphics.font

import dev.luna5ama.trollhack.utils.Nameable
import dev.luna5ama.trollhack.graphics.color.ColorHSVA
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.color.GLColor
import java.awt.Color

/**
 * 2023/5/31 22:34 SagiriXiguajerry
 * I suggest further features use this wrapper. Just forget the features that have already used
 * the original implementations. The compatible layer over the [UnicodeFontRenderer] in this mod
 * and [FontRenderer] in Minecraft will be implemented soon.
 *
 * For efficiency, varying from the current implementation of the fonts' size, this compatible
 * layer will directly use glScale to change the size, in order to compat with MC's FontRenderer.
 * To some extent, it may influence on glyphs' acutance.
 */
abstract class FontRenderer(
    val majorName: String, val unicode: Boolean, val colorFormat: Boolean, val size: Double
) : Nameable, ICompatibleFontRenderer {
    override val name get() = "$majorName+${if (unicode) "unicode" else "acsii"}+${if (colorFormat) "formatted" else "unformatted"}"

    protected abstract fun drawText0(text: CharSequence, x: Double, y: Double, color: ColorRGBA, shadow: Boolean)
    fun drawText(text: CharSequence, x: Double, y: Double, color: ColorRGBA = ColorRGBA.WHITE, shadow: Boolean = true) = drawText0(text, x, y, color, shadow)
    fun drawText(text: CharSequence, x: Float, y: Float, color: ColorRGBA = ColorRGBA.WHITE, shadow: Boolean = true) = drawText(text, x.toDouble(), y.toDouble(), color, shadow)
    fun drawText(text: CharSequence, x: Int, y: Int, color: ColorRGBA = ColorRGBA.WHITE, shadow: Boolean = true) = drawText(text, x.toDouble(), y.toDouble(), color, shadow)
    fun drawText(text: CharSequence, x: Double, y: Double, color: Color, shadow: Boolean = true) = drawText(text, x, y, ColorRGBA(color), shadow)
    fun drawText(text: CharSequence, x: Float, y: Float, color: Color, shadow: Boolean = true) = drawText(text, x.toDouble(), y.toDouble(), color, shadow)
    fun drawText(text: CharSequence, x: Int, y: Int, color: Color, shadow: Boolean = true) = drawText(text, x.toDouble(), y.toDouble(), color, shadow)
    fun drawText(text: CharSequence, x: Double, y: Double, color: ColorHSVA, shadow: Boolean = true) = drawText(text, x, y, color.toRGBA(), shadow)
    fun drawText(text: CharSequence, x: Float, y: Float, color: ColorHSVA, shadow: Boolean = true) = drawText(text, x.toDouble(), y.toDouble(), color, shadow)
    fun drawText(text: CharSequence, x: Int, y: Int, color: ColorHSVA, shadow: Boolean = true) = drawText(text, x.toDouble(), y.toDouble(), color, shadow)
    fun drawText(text: CharSequence, x: Double, y: Double, color: GLColor, shadow: Boolean = true) = drawText(text, x, y, color.toColorRGBA(), shadow)
    fun drawText(text: CharSequence, x: Float, y: Float, color: GLColor, shadow: Boolean = true) = drawText(text, x.toDouble(), y.toDouble(), color, shadow)
    fun drawText(text: CharSequence, x: Int, y: Int, color: GLColor, shadow: Boolean = true) = drawText(text, x.toDouble(), y.toDouble(), color, shadow)
    fun drawText(text: CharSequence, x: Double, y: Double, r: Int, g: Int, b: Int, a: Int = 255, shadow: Boolean = true) = drawText(text, x, y, ColorRGBA(r, g, b, a), shadow)
    fun drawText(text: CharSequence, x: Double, y: Double, r: Float, g: Float, b: Float, a: Float = 255f, shadow: Boolean = true) = drawText(text, x, y, ColorRGBA(r, g, b, a), shadow)
    fun drawText(text: CharSequence, x: Float, y: Float, r: Int, g: Int, b: Int, a: Int = 255, shadow: Boolean = true) = drawText(text, x, y, ColorRGBA(r, g, b, a), shadow)
    fun drawText(text: CharSequence, x: Float, y: Float, r: Float, g: Float, b: Float, a: Float = 255f, shadow: Boolean = true) = drawText(text, x, y, ColorRGBA(r, g, b, a), shadow)
    fun drawText(text: CharSequence, x: Int, y: Int, r: Int, g: Int, b: Int, a: Int = 255, shadow: Boolean = true) = drawText(text, x, y, ColorRGBA(r, g, b, a), shadow)
    fun drawText(text: CharSequence, x: Int, y: Int, r: Float, g: Float, b: Float, a: Float = 255f, shadow: Boolean = true) = drawText(text, x, y, ColorRGBA(r, g, b, a), shadow)

    override fun toString() = name
}