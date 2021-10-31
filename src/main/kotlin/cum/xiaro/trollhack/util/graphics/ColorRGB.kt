package cum.xiaro.trollhack.util.graphics

@JvmInline
value class ColorRGB(val rgba: Int) {
    constructor(r: Int, g: Int, b: Int) :
            this(r, g, b, 255)

    constructor(r: Int, g: Int, b: Int, a: Int) :
            this(
                (r and 255 shl 24) or
                        (g and 255 shl 16) or
                        (b and 255 shl 8) or
                        (a and 255)
            )

    constructor(r: Float, g: Float, b: Float) :
            this((r * 255.0f).toInt(), (g * 255.0f).toInt(), (b * 255.0f).toInt())

    constructor(r: Float, g: Float, b: Float, a: Float) :
            this((r * 255.0f).toInt(), (g * 255.0f).toInt(), (b * 255.0f).toInt(), (a * 255.0f).toInt())


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


    // HSB
    val hue: Float
        get() = ColorUtils.rgbToHue(r, g, b)

    val saturation: Float
        get() = ColorUtils.rgbToSaturation(r, g, b)

    val brightness: Float
        get() = ColorUtils.rgbToBrightness(r, g, b)

    // HSV
    val lightness: Float
        get() = ColorUtils.rgbToLightness(r, g, b)

    // Modification
    fun red(r: Int): ColorRGB {
        return ColorRGB(rgba and 0xFFFFFF or (r shl 24))
    }

    fun green(g: Int): ColorRGB {
        return ColorRGB(rgba and -16711681 or (g shl 16))
    }

    fun blue(b: Int): ColorRGB {
        return ColorRGB(rgba and -65281 or (b shl 8))
    }

    fun alpha(a: Int): ColorRGB {
        return ColorRGB(rgba and -256 or a)
    }

    // Misc
    fun mix(other: ColorRGB, ratio: Float): ColorRGB {
        val rationSelf = 1.0f - ratio
        return ColorRGB(
            (r * rationSelf + other.r * ratio).toInt(),
            (g * rationSelf + other.g * ratio).toInt(),
            (b * rationSelf + other.b * ratio).toInt(),
            (a * rationSelf + other.a * ratio).toInt()
        )
    }

    infix fun mix(other: ColorRGB): ColorRGB {
        return ColorRGB(
            (r + other.r) / 2,
            (g + other.g) / 2,
            (b + other.b) / 2,
            (a + other.a) / 2
        )
    }

    fun toArgb() = ColorUtils.rgbaToArgb(rgba)

    fun toHSB() = ColorUtils.rgbToHSB(r, g, b, a)

    override fun toString(): String {
        return "$r, $g, $b, $a"
    }
}
