package me.luna.trollhack.util.graphics.font.glyph

class CharInfo {
    constructor(textureSize: Float, width: Int, height: Int, posX: Int, posY: Int, extraWidth: Float) {
        this.width = width.toFloat()
        this.height = height.toFloat()
        this.renderWidth = this.width + extraWidth

        val posXFloat = posX.toFloat()
        val posYFloat = posY.toFloat()

        uv = ShortArray(4)
        val multiplier = 65536.0f / textureSize

        uv[0] = (posXFloat * multiplier).toInt().toShort()
        uv[1] = (posYFloat * multiplier).toInt().toShort()
        uv[2] = ((posXFloat + this.width + extraWidth) * multiplier).toInt().toShort()
        uv[3] = ((posYFloat + this.height) * multiplier).toInt().toShort()
    }

    constructor(width: Float, height: Float, renderWidth: Float, u1: Short, v1: Short, u2: Short, v2: Short) {
        this.width = width
        this.height = height
        this.renderWidth = renderWidth
        this.uv = shortArrayOf(u1, v1, u2, v2)
    }

    val width: Float
    val height: Float
    val renderWidth: Float
    val uv: ShortArray
}