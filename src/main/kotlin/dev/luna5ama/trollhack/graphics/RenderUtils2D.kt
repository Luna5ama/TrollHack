package dev.luna5ama.trollhack.graphics

import dev.fastmc.common.toRadians
import dev.luna5ama.trollhack.graphics.buffer.PersistentMappedVBO
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.shaders.Shader
import dev.luna5ama.trollhack.structs.Pos2Color
import dev.luna5ama.trollhack.structs.Pos3Color
import dev.luna5ama.trollhack.structs.sizeof
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.math.MathUtils
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20.glUseProgram
import org.lwjgl.opengl.GL30.glBindVertexArray
import kotlin.math.*

/**
 * Utils for basic 2D shapes rendering
 */
object RenderUtils2D {
    val mc = Wrapper.minecraft
    var vertexSize = 0

    fun drawItem(itemStack: ItemStack, x: Int, y: Int, text: String? = null, drawOverlay: Boolean = true) {
        glUseProgram(0)
        GlStateUtils.blend(true)
        GlStateUtils.depth(true)
        RenderHelper.enableGUIStandardItemLighting()

        mc.renderItem.zLevel = 0.0f
        mc.renderItem.renderItemAndEffectIntoGUI(itemStack, x, y)
        if (drawOverlay) mc.renderItem.renderItemOverlayIntoGUI(mc.fontRenderer, itemStack, x, y, text)
        mc.renderItem.zLevel = 0.0f

        RenderHelper.disableStandardItemLighting()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)

        GlStateUtils.depth(false)
        GlStateUtils.texture2d(true)
    }

    fun drawCircleOutline(
        center: Vec2f = Vec2f.ZERO,
        radius: Float,
        segments: Int = 0,
        lineWidth: Float = 1f,
        color: ColorRGB
    ) {
        drawArcOutline(center, radius, Pair(0f, 360f), segments, lineWidth, color)
    }

    fun drawCircleFilled(center: Vec2f = Vec2f.ZERO, radius: Float, segments: Int = 0, color: ColorRGB) {
        drawArcFilled(center, radius, Pair(0f, 360f), segments, color)
    }

    fun drawArcOutline(
        center: Vec2f = Vec2f.ZERO,
        radius: Float,
        angleRange: Pair<Float, Float>,
        segments: Int = 0,
        lineWidth: Float = 1f,
        color: ColorRGB
    ) {
        val arcVertices = getArcVertices(center, radius, angleRange, segments)
        drawLineStrip(arcVertices, lineWidth, color)
    }

    fun drawArcFilled(
        center: Vec2f = Vec2f.ZERO,
        radius: Float,
        angleRange: Pair<Float, Float>,
        segments: Int = 0,
        color: ColorRGB
    ) {
        val arcVertices = getArcVertices(center, radius, angleRange, segments)
        drawTriangleFan(center, arcVertices, color)
    }

    fun drawRectOutline(width: Float, height: Float, lineWidth: Float = 1.0f, color: ColorRGB) {
        drawRectOutline(0.0f, 0.0f, width, height, lineWidth, color)
    }

    fun drawRectOutline(x1: Float, y1: Float, x2: Float, y2: Float, lineWidth: Float = 1.0f, color: ColorRGB) {
        prepareGL()
        GlStateManager.glLineWidth(lineWidth)

        putVertex(x1, y2, color)
        putVertex(x1, y1, color)
        putVertex(x2, y1, color)
        putVertex(x2, y2, color)
        draw(GL_LINE_LOOP)

        releaseGL()
    }

    fun drawRectFilled(width: Float, height: Float, color: ColorRGB) {
        drawRectFilled(0.0f, 0.0f, width, height, color)
    }

    fun drawRectFilled(x1: Float, y1: Float, x2: Float, y2: Float, color: ColorRGB) {
        prepareGL()

        putVertex(x1, y2, color)
        putVertex(x2, y2, color)
        putVertex(x2, y1, color)
        putVertex(x1, y1, color)

        draw(GL_QUADS)

        releaseGL()
    }

    fun drawQuad(pos1: Vec2f, pos2: Vec2f, pos3: Vec2f, pos4: Vec2f, color: ColorRGB) {
        prepareGL()

        putVertex(pos1, color)
        putVertex(pos2, color)
        putVertex(pos4, color)
        putVertex(pos3, color)

        draw(GL_TRIANGLE_STRIP)

        releaseGL()
    }

    fun drawTriangleOutline(pos1: Vec2f, pos2: Vec2f, pos3: Vec2f, lineWidth: Float = 1f, color: ColorRGB) {
        val vertices = arrayOf(pos1, pos2, pos3)
        drawLineLoop(vertices, lineWidth, color)
    }

    fun drawTriangleFilled(pos1: Vec2f, pos2: Vec2f, pos3: Vec2f, color: ColorRGB) {
        prepareGL()

        putVertex(pos1, color)
        putVertex(pos2, color)
        putVertex(pos3, color)
        draw(GL_TRIANGLES)

        releaseGL()
    }

    fun drawTriangleFan(center: Vec2f, vertices: Array<Vec2f>, color: ColorRGB) {
        prepareGL()

        putVertex(center, color)
        for (vertex in vertices) {
            putVertex(vertex, color)
        }
        draw(GL_TRIANGLE_FAN)

        releaseGL()
    }

    fun drawTriangleStrip(vertices: Array<Vec2f>, color: ColorRGB) {
        prepareGL()

        for (vertex in vertices) {
            putVertex(vertex, color)
        }
        draw(GL_TRIANGLE_STRIP)

        releaseGL()
    }

    fun drawLineLoop(vertices: Array<Vec2f>, lineWidth: Float = 1f, color: ColorRGB) {
        prepareGL()
        GlStateManager.glLineWidth(lineWidth)

        for (vertex in vertices) {
            putVertex(vertex, color)
        }
        draw(GL_LINE_LOOP)

        releaseGL()
        GlStateManager.glLineWidth(1f)
    }

    fun drawLineStrip(vertices: Array<Vec2f>, lineWidth: Float = 1f, color: ColorRGB) {
        prepareGL()
        GlStateManager.glLineWidth(lineWidth)

        for (vertex in vertices) {
            putVertex(vertex, color)
        }
        draw(GL_LINE_STRIP)

        releaseGL()
        GlStateManager.glLineWidth(1f)
    }

    fun drawLine(posBegin: Vec2f, posEnd: Vec2f, lineWidth: Float = 1f, color: ColorRGB) {
        prepareGL()
        GlStateManager.glLineWidth(lineWidth)

        putVertex(posBegin, color)
        putVertex(posEnd, color)
        draw(GL_LINES)

        releaseGL()
        GlStateManager.glLineWidth(1f)
    }

    fun putVertex(pos: Vec2f, color: ColorRGB) {
        putVertex(pos.x, pos.y, color)
    }

    fun putVertex(posX: Float, posY: Float, color: ColorRGB) {
        val array = PersistentMappedVBO.arr
        val struct = Pos2Color(array)
        struct.pos.x = posX
        struct.pos.y = posY
        struct.color = color.rgba
        array += sizeof(Pos3Color)
        vertexSize++
    }

    fun draw(mode: Int) {
        if (vertexSize == 0) return

        DrawShader.bind()
        glBindVertexArray(PersistentMappedVBO.POS2_COLOR)
        glDrawArrays(mode, PersistentMappedVBO.drawOffset, vertexSize)
        PersistentMappedVBO.end()
        glBindVertexArray(0)

        vertexSize = 0
    }

    private fun getArcVertices(
        center: Vec2f,
        radius: Float,
        angleRange: Pair<Float, Float>,
        segments: Int
    ): Array<Vec2f> {
        val range = max(angleRange.first, angleRange.second) - min(angleRange.first, angleRange.second)
        val seg = calcSegments(segments, radius, range)
        val segAngle = (range / seg.toFloat())

        return Array(seg + 1) {
            val angle = (it * segAngle + angleRange.first).toRadians()
            val unRounded = Vec2f(sin(angle), -cos(angle)).times(radius).plus(center)
            Vec2f(MathUtils.round(unRounded.x, 8), MathUtils.round(unRounded.y, 8))
        }
    }

    private fun calcSegments(segmentsIn: Int, radius: Float, range: Float): Int {
        if (segmentsIn != -0) return segmentsIn
        val segments = radius * 0.5 * PI * (range / 360.0)
        return max(segments.roundToInt(), 16)
    }

    fun prepareGL() {
        GlStateUtils.alpha(false)
        GlStateUtils.blend(true)
        GlStateUtils.lineSmooth(true)
        GlStateUtils.cull(false)
    }

    fun releaseGL() {
        GlStateUtils.lineSmooth(false)
        GlStateUtils.cull(true)
    }

    private object DrawShader :
        Shader("/assets/trollhack/shaders/general/Pos2Color.vsh", "/assets/trollhack/shaders/general/Pos2Color.fsh")
}