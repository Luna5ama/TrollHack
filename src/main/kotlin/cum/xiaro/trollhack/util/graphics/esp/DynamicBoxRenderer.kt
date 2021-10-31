package cum.xiaro.trollhack.util.graphics.esp

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.event.AlwaysListening
import cum.xiaro.trollhack.event.events.render.Render3DEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.util.accessor.renderPosX
import cum.xiaro.trollhack.util.accessor.renderPosY
import cum.xiaro.trollhack.util.accessor.renderPosZ
import cum.xiaro.trollhack.util.graphics.MatrixUtils
import cum.xiaro.trollhack.util.graphics.RenderUtils3D
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
class DynamicBoxRenderer : AbstractIndexedRenderer() {
    private var filledSize = 0
    private var outlineSize = 0

    inline fun update(block: Builder.() -> Unit) {
        Builder().apply(block).upload()
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
            StaticBoxRenderer.Shader.alpha(filledAlpha)
            glDrawElements(GL_TRIANGLES, filledSize, GL_UNSIGNED_SHORT, 0L)
        }

        if (outline) {
            StaticBoxRenderer.Shader.alpha(outlineAlpha)
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

        fun putBox(box: AxisAlignedBB, xOffset: Double, yOffset: Double, zOffset: Double, color: ColorRGB, sideMask: SideMask) {
            putBox(box, xOffset, yOffset, zOffset, color, sideMask, sideMask.toOutlineMask())
        }

        fun putBox(box: AxisAlignedBB, xOffset: Double, yOffset: Double, zOffset: Double, color: ColorRGB, sideMask: SideMask, outlineMask: BoxOutlineMask) {
            if (sideMask.mask or outlineMask.mask == 0) {
                return
            } else if (sideMask == SideMask.ALL && outlineMask == BoxOutlineMask.ALL) {
                putBox(box, xOffset, yOffset, zOffset, color)
                return
            }

            val minXD = box.minX - posX
            val minYD = box.minY - posY
            val minZD = box.minZ - posZ
            val maxXD = box.maxX - posX
            val maxYD = box.maxY - posY
            val maxZD = box.maxZ - posZ

            val prevMinX = (minXD - xOffset).toFloat()
            val prevMinY = (minYD - yOffset).toFloat()
            val prevMinZ = (minZD - zOffset).toFloat()
            val prevMaxX = (maxXD - xOffset).toFloat()
            val prevMaxY = (maxYD - yOffset).toFloat()
            val prevMaxZ = (maxZD - zOffset).toFloat()

            val minX = minXD.toFloat()
            val minY = minYD.toFloat()
            val minZ = minZD.toFloat()
            val maxX = maxXD.toFloat()
            val maxY = maxYD.toFloat()
            val maxZ = maxZD.toFloat()

            val vertexMask = sideMask.toVertexMask() + outlineMask.toVertexMask()

            putFilledIndex(vertexMask, sideMask)
            putOutlineIndex(vertexMask, outlineMask)

            val prev = vertexSize

            if (vertexMask.contains(BoxVertexMask.XN_YN_ZN)) {
                putVertex(prevMinX, prevMinY, prevMinZ, minX, minY, minZ)
            }
            if (vertexMask.contains(BoxVertexMask.XN_YN_ZP)) {
                putVertex(prevMinX, prevMinY, prevMaxZ, minX, minY, maxZ)
            }
            if (vertexMask.contains(BoxVertexMask.XN_YP_ZN)) {
                putVertex(prevMinX, prevMaxY, prevMinZ, minX, maxY, minZ)
            }
            if (vertexMask.contains(BoxVertexMask.XN_YP_ZP)) {
                putVertex(prevMinX, prevMaxY, prevMaxZ, minX, maxY, maxZ)
            }
            if (vertexMask.contains(BoxVertexMask.XP_YN_ZN)) {
                putVertex(prevMaxX, prevMinY, prevMinZ, maxX, minY, minZ)
            }
            if (vertexMask.contains(BoxVertexMask.XP_YN_ZP)) {
                putVertex(prevMaxX, prevMinY, prevMaxZ, maxX, minY, maxZ)
            }
            if (vertexMask.contains(BoxVertexMask.XP_YP_ZN)) {
                putVertex(prevMaxX, prevMaxY, prevMinZ, maxX, maxY, minZ)
            }
            if (vertexMask.contains(BoxVertexMask.XP_YP_ZP)) {
                putVertex(prevMaxX, prevMaxY, prevMaxZ, maxX, maxY, maxZ)
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

        fun putBox(box: AxisAlignedBB, xOffset: Double, yOffset: Double, zOffset: Double, color: ColorRGB) {
            val minXD = box.minX - posX
            val minYD = box.minY - posY
            val minZD = box.minZ - posZ
            val maxXD = box.maxX - posX
            val maxYD = box.maxY - posY
            val maxZD = box.maxZ - posZ

            val prevMinX = (minXD - xOffset).toFloat()
            val prevMinY = (minYD - yOffset).toFloat()
            val prevMinZ = (minZD - zOffset).toFloat()
            val prevMaxX = (maxXD - xOffset).toFloat()
            val prevMaxY = (maxYD - yOffset).toFloat()
            val prevMaxZ = (maxZD - zOffset).toFloat()

            val minX = minXD.toFloat()
            val minY = minYD.toFloat()
            val minZ = minZD.toFloat()
            val maxX = maxXD.toFloat()
            val maxY = maxYD.toFloat()
            val maxZ = maxZD.toFloat()

            putFilledIndex()
            putOutlineIndex()

            putVertex(prevMinX, prevMinY, prevMinZ, minX, minY, minZ)
            putVertex(prevMinX, prevMinY, prevMaxZ, minX, minY, maxZ)
            putVertex(prevMinX, prevMaxY, prevMinZ, minX, maxY, minZ)
            putVertex(prevMinX, prevMaxY, prevMaxZ, minX, maxY, maxZ)
            putVertex(prevMaxX, prevMinY, prevMinZ, maxX, minY, minZ)
            putVertex(prevMaxX, prevMinY, prevMaxZ, maxX, minY, maxZ)
            putVertex(prevMaxX, prevMaxY, prevMinZ, maxX, maxY, minZ)
            putVertex(prevMaxX, prevMaxY, prevMaxZ, maxX, maxY, maxZ)

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
        }

        private fun putOutlineIndex() {
            val offset = vertexSize

            outlineIndexList.add(offset.toShort())
            outlineIndexList.add((offset + 1).toShort())

            outlineIndexList.add((offset + 1).toShort())
            outlineIndexList.add((offset + 5).toShort())

            outlineIndexList.add((offset + 5).toShort())
            outlineIndexList.add((offset + 4).toShort())

            outlineIndexList.add((offset + 4).toShort())
            outlineIndexList.add(offset.toShort())


            outlineIndexList.add((offset + 2).toShort())
            outlineIndexList.add((offset + 3).toShort())

            outlineIndexList.add((offset + 3).toShort())
            outlineIndexList.add((offset + 7).toShort())

            outlineIndexList.add((offset + 7).toShort())
            outlineIndexList.add((offset + 6).toShort())

            outlineIndexList.add((offset + 6).toShort())
            outlineIndexList.add((offset + 2).toShort())


            outlineIndexList.add(offset.toShort())
            outlineIndexList.add((offset + 2).toShort())

            outlineIndexList.add((offset + 1).toShort())
            outlineIndexList.add((offset + 3).toShort())

            outlineIndexList.add((offset + 4).toShort())
            outlineIndexList.add((offset + 6).toShort())

            outlineIndexList.add((offset + 5).toShort())
            outlineIndexList.add((offset + 7).toShort())
        }

        private fun putVertex(prevX: Float, prevY: Float, prevZ: Float, x: Float, y: Float, z: Float) {
            posList.add(prevX)
            posList.add(prevY)
            posList.add(prevZ)

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

                glVertexAttribPointer(0, 3, GL_FLOAT, false, 28, 0L)
                glVertexAttribPointer(1, 3, GL_FLOAT, false, 28, 12L)
                glVertexAttribPointer(2, 4, GL_UNSIGNED_BYTE, true, 28, 24L)

                glBindBuffer(GL_ARRAY_BUFFER, 0)

                glEnableVertexAttribArray(0)
                glEnableVertexAttribArray(1)
                glEnableVertexAttribArray(2)

                glBindVertexArray(0)

                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboID)
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, iboBuffer, GL_STREAM_DRAW)
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

                this@DynamicBoxRenderer.posX = posX
                this@DynamicBoxRenderer.posY = posY
                this@DynamicBoxRenderer.posZ = posZ

                this@DynamicBoxRenderer.size = boxSize
                this@DynamicBoxRenderer.filledSize = filledSize
                this@DynamicBoxRenderer.outlineSize = outlineSize
            }
        }

        private fun buildVboBuffer(): ByteBuffer {
            val vboBuffer = GLAllocation.createDirectByteBuffer(vertexSize * 28)

            var colorIndex = 0
            var vertexIndex = 0

            while (colorIndex < intList.size) {
                val color = intList.getInt(colorIndex)
                val size = intList.getInt(colorIndex + 1)

                for (i in 0 until size) {
                    vboBuffer.putFloat(posList.getFloat(vertexIndex++))
                    vboBuffer.putFloat(posList.getFloat(vertexIndex++))
                    vboBuffer.putFloat(posList.getFloat(vertexIndex++))
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

    object Shader : DrawShader("/assets/trollhack/shaders/general/DynamicBoxRenderer.vsh", "/assets/trollhack/shaders/general/Renderer.fsh"), AlwaysListening {
        private val alphaUniform = glGetUniformLocation(id, "alpha")
        private val partialTicksUniform = glGetUniformLocation(id, "partialTicks")

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
                glUniform1f(partialTicksUniform, 1.0f - RenderUtils3D.partialTicks)
            }
        }
    }
}