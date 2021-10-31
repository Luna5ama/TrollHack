package cum.xiaro.trollhack.util.graphics.esp

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.event.AlwaysListening
import cum.xiaro.trollhack.event.events.render.Render3DEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.util.accessor.renderPosX
import cum.xiaro.trollhack.util.accessor.renderPosY
import cum.xiaro.trollhack.util.accessor.renderPosZ
import cum.xiaro.trollhack.util.graphics.MatrixUtils
import cum.xiaro.trollhack.util.graphics.mask.BoxOutlineMask
import cum.xiaro.trollhack.util.graphics.mask.BoxVertexMask
import cum.xiaro.trollhack.util.graphics.mask.SideMask
import cum.xiaro.trollhack.util.graphics.shaders.DrawShader
import cum.xiaro.trollhack.util.threads.onMainThread
import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.shorts.ShortArrayList
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.util.math.AxisAlignedBB
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glGenVertexArrays
import java.nio.ByteBuffer
import java.nio.ShortBuffer

@Suppress("NOTHING_TO_INLINE")
class StaticBoxRenderer : AbstractIndexedRenderer() {
    private var filledSize = 0
    private var outlineSize = 0

    inline fun update(block: Builder.() -> Unit) {
        Builder().apply(block).upload()
    }

    fun update(): Builder {
        return Builder()
    }

    fun render(filledAlpha: Int, outlineAlpha: Int) {
        if (size == 0 || !initialized) return

        val filled = filledAlpha > 0
        val outline = outlineAlpha > 0

        if (!filled && !outline) return

        Shader.bind()
        glBindVertexArray(vaoID)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboID)

        Shader.translate(posX, posY, posZ)

        if (filled) {
            Shader.alpha(filledAlpha)
            glDrawElements(GL_TRIANGLES, filledSize, GL_UNSIGNED_SHORT, 0L)
        }

        if (outline) {
            Shader.alpha(outlineAlpha)
            glDrawElements(GL_LINES, outlineSize, GL_UNSIGNED_SHORT, 2L * filledSize)
        }

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        glBindVertexArray(0)
    }

    inner class Builder {
        private val posList = FloatArrayList()
        private val intList = IntArrayList()
        private val filledIndexList = ShortArrayList()
        private val outlineIndexList = ShortArrayList()

        private val posX = mc.renderManager.renderPosX
        private val posY = mc.renderManager.renderPosY
        private val posZ = mc.renderManager.renderPosZ

        private var boxSize = 0
        private var vertexSize = 0

        fun putBox(box: AxisAlignedBB, color: ColorRGB, sideMask: SideMask) {
            putBox(box, color, sideMask, sideMask.toOutlineMask())
        }

        fun putBox(box: AxisAlignedBB, color: ColorRGB, sideMask: SideMask, outlineMask: BoxOutlineMask) {
            if (sideMask.mask or outlineMask.mask == 0) {
                return
            } else if (sideMask == SideMask.ALL && outlineMask == BoxOutlineMask.ALL) {
                putBox(box, color)
                return
            }

            val minX = (box.minX - posX).toFloat()
            val minY = (box.minY - posY).toFloat()
            val minZ = (box.minZ - posZ).toFloat()
            val maxX = (box.maxX - posX).toFloat()
            val maxY = (box.maxY - posY).toFloat()
            val maxZ = (box.maxZ - posZ).toFloat()

            val vertexMask = sideMask.toVertexMask() + outlineMask.toVertexMask()

            putFilledIndex(vertexMask, sideMask)
            putOutlineIndex(vertexMask, outlineMask)

            val prev = vertexSize

            if (vertexMask.contains(BoxVertexMask.XN_YN_ZN)) {
                putVertex(minX, minY, minZ)
            }
            if (vertexMask.contains(BoxVertexMask.XN_YN_ZP)) {
                putVertex(minX, minY, maxZ)
            }
            if (vertexMask.contains(BoxVertexMask.XN_YP_ZN)) {
                putVertex(minX, maxY, minZ)
            }
            if (vertexMask.contains(BoxVertexMask.XN_YP_ZP)) {
                putVertex(minX, maxY, maxZ)
            }
            if (vertexMask.contains(BoxVertexMask.XP_YN_ZN)) {
                putVertex(maxX, minY, minZ)
            }
            if (vertexMask.contains(BoxVertexMask.XP_YN_ZP)) {
                putVertex(maxX, minY, maxZ)
            }
            if (vertexMask.contains(BoxVertexMask.XP_YP_ZN)) {
                putVertex(maxX, maxY, minZ)
            }
            if (vertexMask.contains(BoxVertexMask.XP_YP_ZP)) {
                putVertex(maxX, maxY, maxZ)
            }

            intList.add(color.rgba)
            intList.add(vertexSize - prev)

            boxSize++
        }

        private fun putFilledIndex(vertexMask: BoxVertexMask, sideMask: SideMask) {
            val offset = vertexSize

            if (sideMask.contains(SideMask.DOWN)) {
                filledIndexList.add(offset.toShort())
                filledIndexList.add((offset + vertexMask.countBits(5)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(1)).toShort())
                filledIndexList.add(offset.toShort())
                filledIndexList.add((offset + vertexMask.countBits(4)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(5)).toShort())
            }

            if (sideMask.contains(SideMask.UP)) {
                filledIndexList.add((offset + vertexMask.countBits(3)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(6)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(2)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(3)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(7)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(6)).toShort())
            }

            if (sideMask.contains(SideMask.NORTH)) {
                filledIndexList.add((offset + vertexMask.countBits(2)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(4)).toShort())
                filledIndexList.add(offset.toShort())
                filledIndexList.add((offset + vertexMask.countBits(2)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(6)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(4)).toShort())
            }

            if (sideMask.contains(SideMask.SOUTH)) {
                filledIndexList.add((offset + vertexMask.countBits(7)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(1)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(5)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(7)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(3)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(1)).toShort())
            }

            if (sideMask.contains(SideMask.WEST)) {
                filledIndexList.add((offset + vertexMask.countBits(3)).toShort())
                filledIndexList.add(offset.toShort())
                filledIndexList.add((offset + vertexMask.countBits(1)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(3)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(2)).toShort())
                filledIndexList.add(offset.toShort())
            }

            if (sideMask.contains(SideMask.EAST)) {
                filledIndexList.add((offset + vertexMask.countBits(6)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(5)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(4)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(6)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(7)).toShort())
                filledIndexList.add((offset + vertexMask.countBits(5)).toShort())
            }
        }

        private fun putOutlineIndex(vertexMask: BoxVertexMask, outlineMask: BoxOutlineMask) {
            val offset = vertexSize

            if (outlineMask.contains(BoxOutlineMask.DOWN_NORTH)) {
                outlineIndexList.add(offset.toShort())
                outlineIndexList.add((offset + vertexMask.countBits(4)).toShort())
            }

            if (outlineMask.contains(BoxOutlineMask.DOWN_SOUTH)) {
                outlineIndexList.add((offset + vertexMask.countBits(1)).toShort())
                outlineIndexList.add((offset + vertexMask.countBits(5)).toShort())
            }

            if (outlineMask.contains(BoxOutlineMask.DOWN_EAST)) {
                outlineIndexList.add((offset + vertexMask.countBits(4)).toShort())
                outlineIndexList.add((offset + vertexMask.countBits(5)).toShort())
            }

            if (outlineMask.contains(BoxOutlineMask.DOWN_WEST)) {
                outlineIndexList.add(offset.toShort())
                outlineIndexList.add((offset + vertexMask.countBits(1)).toShort())
            }


            if (outlineMask.contains(BoxOutlineMask.UP_NORTH)) {
                outlineIndexList.add((offset + vertexMask.countBits(2)).toShort())
                outlineIndexList.add((offset + vertexMask.countBits(6)).toShort())
            }

            if (outlineMask.contains(BoxOutlineMask.UP_SOUTH)) {
                outlineIndexList.add((offset + vertexMask.countBits(3)).toShort())
                outlineIndexList.add((offset + vertexMask.countBits(7)).toShort())
            }

            if (outlineMask.contains(BoxOutlineMask.UP_WEST)) {
                outlineIndexList.add((offset + vertexMask.countBits(2)).toShort())
                outlineIndexList.add((offset + vertexMask.countBits(3)).toShort())
            }

            if (outlineMask.contains(BoxOutlineMask.UP_EAST)) {
                outlineIndexList.add((offset + vertexMask.countBits(6)).toShort())
                outlineIndexList.add((offset + vertexMask.countBits(7)).toShort())
            }


            if (outlineMask.contains(BoxOutlineMask.NORTH_WEST)) {
                outlineIndexList.add(offset.toShort())
                outlineIndexList.add((offset + vertexMask.countBits(2)).toShort())
            }

            if (outlineMask.contains(BoxOutlineMask.NORTH_EAST)) {
                outlineIndexList.add((offset + vertexMask.countBits(4)).toShort())
                outlineIndexList.add((offset + vertexMask.countBits(6)).toShort())
            }

            if (outlineMask.contains(BoxOutlineMask.SOUTH_WEST)) {
                outlineIndexList.add((offset + vertexMask.countBits(1)).toShort())
                outlineIndexList.add((offset + vertexMask.countBits(3)).toShort())
            }

            if (outlineMask.contains(BoxOutlineMask.SOUTH_EAST)) {
                outlineIndexList.add((offset + vertexMask.countBits(5)).toShort())
                outlineIndexList.add((offset + vertexMask.countBits(7)).toShort())
            }
        }

        fun putBox(box: AxisAlignedBB, color: ColorRGB) {
            val minX = (box.minX - posX).toFloat()
            val minY = (box.minY - posY).toFloat()
            val minZ = (box.minZ - posZ).toFloat()
            val maxX = (box.maxX - posX).toFloat()
            val maxY = (box.maxY - posY).toFloat()
            val maxZ = (box.maxZ - posZ).toFloat()

            putFilledIndex()
            putOutlineIndex()

            putVertex(minX, minY, minZ)
            putVertex(minX, minY, maxZ)
            putVertex(minX, maxY, minZ)
            putVertex(minX, maxY, maxZ)
            putVertex(maxX, minY, minZ)
            putVertex(maxX, minY, maxZ)
            putVertex(maxX, maxY, minZ)
            putVertex(maxX, maxY, maxZ)

            intList.add(color.rgba)
            intList.add(8)

            boxSize++
        }

        private fun putFilledIndex() {
            val offset = vertexSize

            filledIndexList.add(offset.toShort())
            filledIndexList.add((offset + 5).toShort())
            filledIndexList.add((offset + 1).toShort())
            filledIndexList.add(offset.toShort())
            filledIndexList.add((offset + 4).toShort())
            filledIndexList.add((offset + 5).toShort())

            filledIndexList.add((offset + 3).toShort())
            filledIndexList.add((offset + 6).toShort())
            filledIndexList.add((offset + 2).toShort())
            filledIndexList.add((offset + 3).toShort())
            filledIndexList.add((offset + 7).toShort())
            filledIndexList.add((offset + 6).toShort())

            filledIndexList.add((offset + 3).toShort())
            filledIndexList.add(offset.toShort())
            filledIndexList.add((offset + 1).toShort())
            filledIndexList.add((offset + 3).toShort())
            filledIndexList.add((offset + 2).toShort())
            filledIndexList.add(offset.toShort())

            filledIndexList.add((offset + 6).toShort())
            filledIndexList.add((offset + 5).toShort())
            filledIndexList.add((offset + 4).toShort())
            filledIndexList.add((offset + 6).toShort())
            filledIndexList.add((offset + 7).toShort())
            filledIndexList.add((offset + 5).toShort())

            filledIndexList.add((offset + 2).toShort())
            filledIndexList.add((offset + 4).toShort())
            filledIndexList.add(offset.toShort())
            filledIndexList.add((offset + 2).toShort())
            filledIndexList.add((offset + 6).toShort())
            filledIndexList.add((offset + 4).toShort())

            filledIndexList.add((offset + 7).toShort())
            filledIndexList.add((offset + 1).toShort())
            filledIndexList.add((offset + 5).toShort())
            filledIndexList.add((offset + 7).toShort())
            filledIndexList.add((offset + 3).toShort())
            filledIndexList.add((offset + 1).toShort())
        }

        private fun putOutlineIndex() {
            val offset = vertexSize

            outlineIndexList.add(offset.toShort())
            outlineIndexList.add((offset + 4).toShort())

            outlineIndexList.add((offset + 1).toShort())
            outlineIndexList.add((offset + 5).toShort())

            outlineIndexList.add(offset.toShort())
            outlineIndexList.add((offset + 1).toShort())

            outlineIndexList.add((offset + 4).toShort())
            outlineIndexList.add((offset + 5).toShort())


            outlineIndexList.add((offset + 2).toShort())
            outlineIndexList.add((offset + 6).toShort())

            outlineIndexList.add((offset + 3).toShort())
            outlineIndexList.add((offset + 7).toShort())

            outlineIndexList.add((offset + 2).toShort())
            outlineIndexList.add((offset + 3).toShort())

            outlineIndexList.add((offset + 6).toShort())
            outlineIndexList.add((offset + 7).toShort())


            outlineIndexList.add(offset.toShort())
            outlineIndexList.add((offset + 2).toShort())

            outlineIndexList.add((offset + 1).toShort())
            outlineIndexList.add((offset + 3).toShort())

            outlineIndexList.add((offset + 4).toShort())
            outlineIndexList.add((offset + 6).toShort())

            outlineIndexList.add((offset + 5).toShort())
            outlineIndexList.add((offset + 7).toShort())
        }

        private fun putVertex(x: Float, y: Float, z: Float) {
            posList.add(x)
            posList.add(y)
            posList.add(z)
            vertexSize++
        }

        fun upload() {
            val vboBuffer = buildVboBuffer()
            val iboBuffer = buildIboBuffer()

            val filledSize = filledIndexList.size
            val outlineSize = outlineIndexList.size

            onMainThread {
                if (vaoID == 0) vaoID = glGenVertexArrays()

                if (initialized) {
                    glDeleteBuffers(vboID)
                    glDeleteBuffers(iboID)
                }

                vboID = glGenBuffers()
                iboID = glGenBuffers()

                glBindVertexArray(vaoID)

                glBindBuffer(GL_ARRAY_BUFFER, vboID)
                glBufferData(GL_ARRAY_BUFFER, vboBuffer, GL_STREAM_DRAW)

                glVertexAttribPointer(0, 3, GL_FLOAT, false, 16, 0L)
                glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, true, 16, 12L)

                glBindBuffer(GL_ARRAY_BUFFER, 0)

                glEnableVertexAttribArray(0)
                glEnableVertexAttribArray(1)

                glBindVertexArray(0)

                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboID)
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, iboBuffer, GL_STREAM_DRAW)
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

                this@StaticBoxRenderer.posX = posX
                this@StaticBoxRenderer.posY = posY
                this@StaticBoxRenderer.posZ = posZ

                this@StaticBoxRenderer.size = boxSize
                this@StaticBoxRenderer.filledSize = filledSize
                this@StaticBoxRenderer.outlineSize = outlineSize
            }
        }

        private fun buildVboBuffer(): ByteBuffer {
            val vboBuffer = GLAllocation.createDirectByteBuffer(vertexSize * 16)

            var colorIndex = 0
            var vertexIndex = 0

            while (colorIndex < intList.size) {
                val color = intList.getInt(colorIndex)
                val size = intList.getInt(colorIndex + 1)

                for (i in 0 until size) {
                    vboBuffer.putFloat(posList.getFloat(vertexIndex++))
                    vboBuffer.putFloat(posList.getFloat(vertexIndex++))
                    vboBuffer.putFloat(posList.getFloat(vertexIndex++))
                    vboBuffer.putInt(color)
                }

                colorIndex += 2
            }

            vboBuffer.flip()
            return vboBuffer
        }

        private fun buildIboBuffer(): ShortBuffer {
            val iboBuffer = GLAllocation.createDirectByteBuffer((filledIndexList.size + outlineIndexList.size) * 2).asShortBuffer()

            iboBuffer.put(filledIndexList.elements(), 0, filledIndexList.size)
            iboBuffer.put(outlineIndexList.elements(), 0, outlineIndexList.size)

            iboBuffer.flip()
            return iboBuffer
        }
    }

    object Shader : DrawShader("/assets/trollhack/shaders/general/StaticBoxRenderer.vsh", "/assets/trollhack/shaders/general/Renderer.fsh"), AlwaysListening {
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