package dev.luna5ama.trollhack.graphics.texture

import dev.luna5ama.trollhack.graphics.GLHelper
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects.draw
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP

interface Texture {
    val id: Int
    var width: Int
    var height: Int
    val available: Boolean
    var msaaLevel: Int // 0 -> Don't use msaa, >=1 -> Use msaa
    fun bindTexture()
    fun unbindTexture()
    fun deleteTexture()
}

inline fun <T : Texture> T.useTexture(
    block: Texture.() -> Unit
): T {
    bindTexture()
    block()
    unbindTexture()
    return this
}

fun <T : Texture> T.drawTexture(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    u: Int = 0,
    v: Int = 0,
    u1: Int = width,
    v1: Int = height,
    colorRGB: ColorRGBA = ColorRGBA.WHITE
) = useTexture {
    val startU = u / width.toFloat()
    val endU = u1 / width.toFloat()
    val startV = v / height.toFloat()
    val endV = v1 / height.toFloat()
    GLHelper.blend = true
    GL_TRIANGLE_STRIP.draw(PMVBObjects.VertexMode.Universal) {
        universal(endX, startY, endU, startV, colorRGB)
        universal(startX, startY, startU, startV, colorRGB)
        universal(endX, endY, endU, endV, colorRGB)
        universal(startX, endY, startU, endV, colorRGB)
    }
}
