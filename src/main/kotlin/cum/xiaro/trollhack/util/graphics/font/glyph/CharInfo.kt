package cum.xiaro.trollhack.util.graphics.font.glyph

class CharInfo(
    textureSize: Float,
    width: Int,
    height: Int,
    posX: Int,
    posY: Int,
    extraWidth: Float
) {
    val width = width.toFloat()
    val height = height.toFloat()
    val renderWidth = this.width + extraWidth
    val u1: Float
    val v1: Float
    val u2: Float
    val v2: Float

    init {
        val posXFloat = posX.toFloat()
        val posYFloat = posY.toFloat()

        u1 = posXFloat / textureSize
        v1 = posYFloat / textureSize
        u2 = (posXFloat + this.width + extraWidth) / textureSize
        v2 = (posYFloat + this.height) / textureSize
    }
}