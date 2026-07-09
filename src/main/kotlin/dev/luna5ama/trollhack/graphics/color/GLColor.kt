package dev.luna5ama.trollhack.graphics.color

data class GLColor(val r: Float, val g: Float, val b: Float, val a: Float) {
    fun toColorRGBA() = ColorRGBA(r, g, b, a)
}
