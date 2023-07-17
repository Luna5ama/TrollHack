package dev.luna5ama.trollhack.graphics.font.glyph

data class CharInfo(
    val width: Float,
    val height: Float,
    val renderWidth: Float,
    val uv: ShortArray
) {
    constructor(
        textureSize: Float,
        width: Int,
        height: Int,
        posX: Int,
        posY: Int,
        extraWidth: Float
    ) : this(width.toFloat(), height.toFloat(), width + extraWidth, ShortArray(4)) {
        val posXFloat = posX.toFloat()
        val posYFloat = posY.toFloat()
        val multiplier = 65536.0f / textureSize

        uv[0] = (posXFloat * multiplier).toInt().toShort()
        uv[1] = (posYFloat * multiplier).toInt().toShort()
        uv[2] = ((posXFloat + this.width + extraWidth) * multiplier).toInt().toShort()
        uv[3] = ((posYFloat + this.height) * multiplier).toInt().toShort()
    }

    constructor(width: Float, height: Float, renderWidth: Float, u1: Short, v1: Short, u2: Short, v2: Short) : this(
        width,
        height,
        renderWidth,
        shortArrayOf(u1, v1, u2, v2)
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CharInfo) return false

        if (width != other.width) return false
        if (height != other.height) return false
        if (renderWidth != other.renderWidth) return false
        return uv.contentEquals(other.uv)
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + renderWidth.hashCode()
        result = 31 * result + uv.contentHashCode()
        return result
    }
}