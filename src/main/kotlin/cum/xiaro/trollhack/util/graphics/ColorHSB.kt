package cum.xiaro.trollhack.util.graphics

data class ColorHSB(
    val h: Float,
    val s: Float,
    val b: Float,
    val a: Float
) {
    constructor(h: Float, s: Float, b: Float) : this(h, s, b, 1.0f)

    fun toRGB() = ColorUtils.hsbToRGB(h, s, b, a)

    override fun toString(): String {
        return "$h, $s, $b, $a"
    }
}