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

@JvmInline
value class ColorRGBA(val rgba: Int) : ColorSpace.RGBA {
    constructor(r: Int, g: Int, b: Int) :
            this(r, g, b, 255)

    constructor(r: Int, g: Int, b: Int, a: Int) :
            this(
                (r and 255 shl 24) or
                        (g and 255 shl 16) or
                        (b and 255 shl 8) or
                        (a and 255)
            )

    constructor(color: Color) : this(color.red, color.green, color.blue, color.alpha)

    constructor(r: Float, g: Float, b: Float) :
            this((r * 255.0f).toInt(), (g * 255.0f).toInt(), (b * 255.0f).toInt())

    constructor(r: Float, g: Float, b: Float, a: Float) :
            this((r * 255.0f).toInt(), (g * 255.0f).toInt(), (b * 255.0f).toInt(), (a * 255.0f).toInt())

    val awt get() = Color(r, g, b, a)

    // Int color
    val r: Int
        get() = rgba shr 24 and 255

    val g: Int
        get() = rgba shr 16 and 255

    val b: Int
        get() = rgba shr 8 and 255

    val a: Int
        get() = rgba and 255


    // Float color
    val rFloat: Float
        get() = r / 255.0f

    val gFloat: Float
        get() = g / 255.0f

    val bFloat: Float
        get() = b / 255.0f

    val aFloat: Float
        get() = a / 255.0f


    // Modification
    fun red(r: Int): ColorRGBA {
        return ColorRGBA(r, g, b, a)
    }

    fun green(g: Int): ColorRGBA {
        return ColorRGBA(r, g, b, a)
    }

    fun blue(b: Int): ColorRGBA {
        return ColorRGBA(r, g, b, a)
    }

    fun alpha(a: Int): ColorRGBA {
        return ColorRGBA(r, g, b, a)
    }

    // Misc
    fun mix(other: ColorRGBA, ratio: Float): ColorRGBA {
        val rationSelf = 1.0f - ratio
        return ColorRGBA(
            (r * rationSelf + other.r * ratio).toInt(),
            (g * rationSelf + other.g * ratio).toInt(),
            (b * rationSelf + other.b * ratio).toInt(),
            (a * rationSelf + other.a * ratio).toInt()
        )
    }

    infix fun mix(other: ColorRGBA): ColorRGBA {
        return ColorRGBA(
            (r + other.r) / 2,
            (g + other.g) / 2,
            (b + other.b) / 2,
            (a + other.a) / 2
        )
    }

    fun toHSB() = ColorUtils.rgbToHSB(r, g, b, a)
    fun toGL() = GLColor(rFloat, gFloat, bFloat, aFloat)

    fun pure() = toHSB().brightness(1f).saturation(1f).alpha(1f).toRGBA()

    operator fun component1() = r
    operator fun component2() = g
    operator fun component3() = b
    operator fun component4() = a

    companion object {
        val WHITE = Color.WHITE.toColorRGBA()
        val BLACK = Color.BLACK.toColorRGBA()
        val GOLD = ColorRGB(250, 170, 0)
        val LIGHT_GRAY = Color.LIGHT_GRAY.toColorRGBA()
        val GRAY = Color.GRAY.toColorRGBA()
        val DARK_GRAY = Color.DARK_GRAY.toColorRGBA()
        val RED = Color.RED.toColorRGBA()
        val PINK = Color.PINK.toColorRGBA()
        val ORANGE = Color.ORANGE.toColorRGBA()
        val YELLOW = Color.YELLOW.toColorRGBA()
        val GREEN = Color.GREEN.toColorRGBA()
        val MAGENTA = Color.MAGENTA.toColorRGBA()
        val CYAN = Color.CYAN.toColorRGBA()
        val BLUE = Color.BLUE.toColorRGBA()

        fun fromString(str: String) : ColorRGBA {
            val (r, g, b, a) = str.split(",").map { it.trimIndent().toInt() }
            return ColorRGBA(r, g, b, a)
        }

        private fun Color.toColorRGBA() = ColorRGBA(red, green, blue, alpha)
    }

    override fun toString(): String {
        return "$r, $g, $b, $a"
    }
}