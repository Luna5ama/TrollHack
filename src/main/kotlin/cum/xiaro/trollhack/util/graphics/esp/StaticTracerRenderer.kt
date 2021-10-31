package cum.xiaro.trollhack.util.graphics.esp

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.event.AlwaysListening
import cum.xiaro.trollhack.event.events.render.Render3DEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.util.accessor.renderPosX
import cum.xiaro.trollhack.util.accessor.renderPosY
import cum.xiaro.trollhack.util.accessor.renderPosZ
import cum.xiaro.trollhack.util.graphics.MatrixUtils
import cum.xiaro.trollhack.util.graphics.shaders.DrawShader
import cum.xiaro.trollhack.util.math.xCenter
import cum.xiaro.trollhack.util.math.yCenter
import cum.xiaro.trollhack.util.math.zCenter
import cum.xiaro.trollhack.util.threads.onMainThread
import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.util.math.AxisAlignedBB
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glGenVertexArrays
import java.nio.ByteBuffer

class StaticTracerRenderer : AbstractRenderer() {
    fun render(tracerAlpha: Int) {
        if (size == 0 || !initialized) return

        val tracer = tracerAlpha > 0

        if (tracer) {
            Shader.bind()
            glBindVertexArray(vaoID)

            Shader.translate(posX, posY, posZ)
            Shader.alpha(tracerAlpha)

            glDrawArrays(GL_LINES, 0, size * 2)

            glBindVertexArray(0)
        }
    }

    inline fun update(block: StaticTracerRenderer.Builder.() -> Unit) {
        Builder().apply(block).upload()
    }

    inner class Builder {
        private val posList = FloatArrayList()
        private val colorList = IntArrayList()

        private val posX = mc.renderManager.renderPosX
        private val posY = mc.renderManager.renderPosY
        private val posZ = mc.renderManager.renderPosZ

        private var size = 0

        fun putTracer(box: AxisAlignedBB, color: ColorRGB) {
            putTracer(box.xCenter, box.yCenter, box.zCenter, color)
        }

        fun putTracer(x: Double, y: Double, z: Double, color: ColorRGB) {
            posList.add((x - posX).toFloat())
            posList.add((y - posY).toFloat())
            posList.add((z - posZ).toFloat())
            colorList.add(color.rgba)

            size++
        }

        fun upload() {
            val vboBuffer = buildVboBuffer()

            onMainThread {
                if (vaoID == 0) vaoID = glGenVertexArrays()

                if (initialized) {
                    glDeleteBuffers(vboID)
                }

                vboID = glGenBuffers()

                glBindVertexArray(vaoID)

                glBindBuffer(GL_ARRAY_BUFFER, vboID)
                glBufferData(GL_ARRAY_BUFFER, vboBuffer, GL_STREAM_DRAW)

                glVertexAttribPointer(0, 3, GL_FLOAT, false, 16, 0L)
                glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, true, 16, 12L)

                glBindBuffer(GL_ARRAY_BUFFER, 0)

                glEnableVertexAttribArray(0)
                glEnableVertexAttribArray(1)

                glBindVertexArray(0)

                this@StaticTracerRenderer.posX = posX
                this@StaticTracerRenderer.posY = posY
                this@StaticTracerRenderer.posZ = posZ

                this@StaticTracerRenderer.size = size
            }
        }

        private fun buildVboBuffer(): ByteBuffer {
            val vboBuffer = GLAllocation.createDirectByteBuffer(size * 2 * 16)

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

    object Shader : DrawShader("/assets/trollhack/shaders/general/StaticTracerRenderer.vsh", "/assets/trollhack/shaders/general/Renderer.fsh"), AlwaysListening {
        private val alphaUniform = glGetUniformLocation(id, "alpha")

        fun translate(xOffset: Double, yOffset: Double, zOffset: Double) {
            val x = xOffset - mc.renderManager.renderPosX
            val y = yOffset - mc.renderManager.renderPosY
            val z = zOffset - mc.renderManager.renderPosZ

            val modelView = MatrixUtils.loadModelViewMatrix().getMatrix()
                .translate(x.toFloat(), y.toFloat(), z.toFloat())

            updateModelViewMatrix(modelView)
        }

        fun alpha(value: Int) {
            glUniform1f(alphaUniform, value / 255.0f)
        }

        init {
            listener<Render3DEvent>(Int.MAX_VALUE - 1, true) {
                bind()
                updateProjectionMatrix()
            }
        }
    }
}