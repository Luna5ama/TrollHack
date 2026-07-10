package dev.luna5ama.trollhack.graphics.font

import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import java.awt.Color

interface ICompatibleFontRenderer : IGradientFontRenderer {
    val disableCache: Boolean
    val height: Float

    val statistics: Statistics

    fun cache(value: Boolean)

    fun drawAtlas(index: Int)
    fun drawStaticString(string: String)
    fun drawShadowedStaticString(string: String)

    fun setScale(scale: Float)
    fun getHeight(scale: Float): Float
    fun getWidth(text: String): Float
    fun getWidth(text: String, scale: Float): Float

    fun drawString(text: String, color: ColorRGBA)
    fun drawString(text: String, x: Float, y: Float)
    fun drawString(text: String, x: Float, y: Float, color: Int)
    fun drawString(text: String, x: Float, y: Float, color: Color)
    fun drawString(text: String, x: Float, y: Float, color: Color, scale: Float)

    fun drawStringWithShadow(text: String, x: Float, y: Float)
    fun drawStringWithShadow(text: String, x: Float, y: Float, color: ColorRGBA)
    fun drawStringWithShadow(text: String, color: ColorRGBA)
    fun drawStringWithShadow(text: String, x: Float, y: Float, color: Color)
    fun drawStringWithShadow(text: String, x: Float, y: Float, color: Color, scale: Float)
    fun drawStringWithShadow(text: String, x: Float, y: Float, shadowDepth: Float)
    fun drawStringWithShadow(text: String, x: Float, y: Float, shadowDepth: Float, color: Color)
    fun drawStringWithShadow(text: String, x: Float, y: Float, shadowDepth: Float, color: Color, scale: Float)

    fun drawString0(
        text0: String,
        x: Float, y: Float,
        color0: Color, gradient: Boolean, colors: Array<Color>,
        scale0: Float, loop: Boolean, shadow: Boolean
    )
    fun drawStringShadowed0(text0: String, x: Float, y: Float, color0: Color, scale0: Float)
    fun drawString0(text0: String, x: Float, y: Float, color0: Color, scale0: Float, shadow: Boolean)

    fun refresh()

}
