/*
 * Copyright (c) 2021-2022, SagiriXiguajerry. All rights reserved.
 * This repository will be transformed to SuperMic_233.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package dev.luna5ama.trollhack.graphics.color

import java.awt.Color
import kotlin.math.ceil

internal object ColorUtils {
    const val ONE_THIRD = 1.0f / 3.0f
    const val TWO_THIRD = 2.0f / 3.0f

    fun argbToRgba(argb: Int) =
        (argb and 0xFFFFFF shl 8) or
                (argb shr 24 and 255)

    fun rgbaToArgb(rgba: Int) =
        (rgba shr 8 and 0xFFFFFF) or
                (rgba and 255 shl 24)

    fun toARGB(r: Int, g: Int, b: Int, a: Int): Int {
        return Color(r, g, b, a).rgb
    }

    @JvmOverloads
    fun toRGBA(r: Int, g: Int, b: Int, a: Int = 255): Int {
        return (r shl 16) + (g shl 8) + b + (a shl 24)
    }

    fun toRGBA(r: Float, g: Float, b: Float, a: Float): Int {
        return toRGBA((r * 255.0f).toInt(), (g * 255.0f).toInt(), (b * 255.0f).toInt(), (a * 255.0f).toInt())
    }

    fun rainbow(delay: Int, saturation: Int, brightness: Int): Color {
        val rainbowState = ceil((System.currentTimeMillis() + delay) / 20.0)
        return Color.getHSBColor(
            (rainbowState % 360.0 / 360.0).toFloat(),
            saturation.toFloat() / 255.0f,
            brightness.toFloat() / 255.0f
        )
    }

    fun toRGBA(colors: FloatArray): Int {
        require(colors.size == 4) { "colors[] must have a length of 4!" }
        return toRGBA(
            colors[0],
            colors[1], colors[2], colors[3]
        )
    }

    fun toRGBA(colors: DoubleArray): Int {
        require(colors.size == 4) { "colors[] must have a length of 4!" }
        return toRGBA(colors[0].toFloat(), colors[1].toFloat(), colors[2].toFloat(), colors[3].toFloat())
    }

    fun toRGBA(color: Color): Int {
        return toRGBA(color.red, color.green, color.blue, color.alpha)
    }

    fun rgbToHSB(r: Int, g: Int, b: Int, a: Int): ColorHSVA {
//        val cMax = maxOf(r, g, b)
//        if (cMax == 0) return ColorHSB(0f, 0f, 0f, a / 255f)
//
//        val cMin = minOf(r, g, b)
//        val diff = cMax - cMin
//
//        val diff6 = diff * 6f
//
//        val hue = when (cMax) {
//            cMin -> 0f
//            r -> (g - b) / diff6 + 1f
//            g -> (b - r) / diff6 + (1f / 3f)
//            else -> (r - g) / diff6 + (2f / 3f)
//        } % 1f
//
//        val saturation = diff / cMax.toFloat()
//        val brightness = cMax / 255f
//
//        return ColorHSB(hue, saturation, brightness, a / 255f)
        val cMax = maxOf(r, g, b)
        if (cMax == 0) return ColorHSVA(0.0f, 0.0f, 0.0f, a / 255.0f)
        val cMin = minOf(r, g, b)
        val diff = cMax - cMin
        val diff6 = diff * 6.0f
        var hue = when (cMax) {
            cMin -> {
                0.0f
            }
            r -> {
                (g - b) / diff6 + 1.0f
            }
            g -> {
                (b - r) / diff6 + ONE_THIRD
            }
            else -> {
                (r - g) / diff6 + TWO_THIRD
            }
        }
        hue %= 1.0f
        val saturation = diff / cMax.toFloat()
        val brightness = cMax / 255.0f
        return ColorHSVA(hue, saturation, brightness, a / 255.0f)
    }

    fun hsbToRGB(h: Float, s: Float, b: Float, a: Float): ColorRGBA {
        val hue6 = (h % 1f) * 6f
        val intHue6 = hue6.toInt()
        val f = hue6 - intHue6
        val p = b * (1f - s)
        val q = b * (1f - f * s)
        val t = b * (1f - (1f - f) * s)
        return when (intHue6) {
            0 -> ColorRGBA(b, t, p, a)
            1 -> ColorRGBA(q, b, p, a)
            2 -> ColorRGBA(p, b, t, a)
            3 -> ColorRGBA(p, q, b, a)
            4 -> ColorRGBA(t, p, b, a)
            5 -> ColorRGBA(b, p, q, a)
            else -> ColorRGBA(255, 255, 255)
        }
    }
}