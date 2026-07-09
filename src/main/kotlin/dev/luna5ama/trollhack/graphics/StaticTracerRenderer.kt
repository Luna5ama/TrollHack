package dev.luna5ama.trollhack.graphics

import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.ints.IntArrayList
import dev.luna5ama.trollhack.RS
import dev.luna5ama.trollhack.RenderSystem
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.matrix.scope
import dev.luna5ama.trollhack.graphics.matrix.translatef
import dev.luna5ama.trollhack.graphics.shader.Shader
import dev.luna5ama.trollhack.utils.createDirectByteBuffer
import dev.luna5ama.trollhack.utils.threads.RenderThreadExecutor
import net.minecraft.world.phys.AABB
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL41.glProgramUniform1f
import org.lwjgl.opengl.GL41.glProgramUniformMatrix4fv
import org.lwjgl.opengl.GL45
import org.lwjgl.opengl.GL45.glCreateBuffers
import org.lwjgl.opengl.GL45.glCreateVertexArrays
import org.lwjgl.opengl.GL45.glNamedBufferData
import org.lwjgl.opengl.GL45.glVertexArrayVertexBuffer
import java.nio.ByteBuffer

class StaticTracerRenderer : AbstractRenderer() {
    fun render(tracerAlpha: Int) {
        if (size == 0 || !initialized) return

        val tracer = tracerAlpha > 0

        if (tracer) {
            RenderSystem.matrixLayer.scope {
                DrawShader.bind()
                glBindVertexArray(vaoID)

                translatef(posX.toFloat(), posY.toFloat(), posZ.toFloat())

                DrawShader.updateMatrix()
                DrawShader.alpha(tracerAlpha)

                glDrawArrays(GL_LINES, 0, size * 2)

                glBindVertexArray(0)
            }
        }
    }

    inline fun update(block: Builder.() -> Unit) {
        Builder().apply(block).upload()
    }

    inner class Builder {
        private val posList = FloatArrayList()
        private val colorList = IntArrayList()

        private val posX = mc.gameRenderer.mainCamera.position().x
        private val posY = mc.gameRenderer.mainCamera.position().y
        private val posZ = mc.gameRenderer.mainCamera.position().z

        private var size = 0

        fun putTracer(box: AABB, color: ColorRGBA) {
            putTracer(box.center.x, box.center.y, box.center.z, color)
        }

        fun putTracer(x: Double, y: Double, z: Double, color: ColorRGBA) {
            posList.add((x - posX).toFloat())
            posList.add((y - posY).toFloat())
            posList.add((z - posZ).toFloat())
            colorList.add(color.rgba)

            size++
        }

        fun upload() {
            val vboBuffer = buildVboBuffer()

            RenderThreadExecutor.execute {
                if (vaoID == 0) vaoID = glCreateVertexArrays()

                if (initialized) {
                    glDeleteBuffers(vboID)
                }

                vboID = glCreateBuffers()

                glNamedBufferData(vboID, vboBuffer, GL_STREAM_DRAW)
                glVertexArrayVertexBuffer(vaoID, 0, vboID, 0, 16)

                GL45.glEnableVertexArrayAttrib(vaoID, 0)
                GL45.glVertexArrayAttribBinding(vaoID, 0, 0)
                GL45.glVertexArrayAttribFormat(vaoID, 0, 3, GL_FLOAT, false, 0)

                GL45.glEnableVertexArrayAttrib(vaoID, 1)
                GL45.glVertexArrayAttribBinding(vaoID, 1, 0)
                GL45.glVertexArrayAttribFormat(vaoID, 1, 4, GL_UNSIGNED_BYTE, true, 12)

                this@StaticTracerRenderer.posX = posX
                this@StaticTracerRenderer.posY = posY
                this@StaticTracerRenderer.posZ = posZ

                this@StaticTracerRenderer.size = size
            }
        }

        private fun buildVboBuffer(): ByteBuffer {
            val vboBuffer = createDirectByteBuffer(size * 2 * 16)

            var vertexIndex = 0

            for (i in colorList.indices) {
                vboBuffer.position(vboBuffer.position() + 12)
                vboBuffer.putInt(colorList.getInt(i))

                vboBuffer.putFloat(posList.getFloat(vertexIndex++))
                vboBuffer.putFloat(posList.getFloat(vertexIndex++))
                vboBuffer.putFloat(posList.getFloat(vertexIndex++))
                vboBuffer.putInt(colorList.getInt(i))
            }

            vboBuffer.flip()
            return vboBuffer
        }
    }

    object DrawShader : Shader(
        "/assets/shader/general/StaticTracerRenderer.vsh",
        "/assets/shader/general/Renderer.fsh"
    ) {
        private val mvpUniform = glGetUniformLocation(id, "mvp")
        private val alphaUniform = glGetUniformLocation(id, "alpha")

        fun updateMatrix() {
            glProgramUniformMatrix4fv(id, mvpUniform, false, RS.matrixLayer.matrixArray)
        }

        fun alpha(value: Int) {
            glProgramUniform1f(id, alphaUniform, value / 255.0f)
        }
    }
}