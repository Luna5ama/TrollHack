package dev.luna5ama.trollhack.graphics.color

data class ColorHSVA(
    val h: Float,
    val s: Float,
    val b: Float,
    val a: Float
) {
    constructor(h: Float, s: Float, b: Float) : this(h, s, b, 1.0f)
    constructor(h: Int, s: Int, b: Int, a: Int = 255) : this(h / 255f, s / 255f, b / 255f, a / 255f)

    fun hue(h: Float) = ColorHSVA(h, this.s, this.b, this.a)
    fun saturation(s: Float) = ColorHSVA(this.h, s, this.b, this.a)
    fun brightness(b: Float) = ColorHSVA(this.h, this.s, b, this.a)
    fun alpha(a: Float) = ColorHSVA(this.h, this.s, this.b, a)

    fun toRGBA() = ColorUtils.hsbToRGB(h, s, b, a)
}
