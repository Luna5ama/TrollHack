package dev.luna5ama.trollhack.graphics

import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.shorts.ShortArrayList
import dev.luna5ama.trollhack.RS
import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.impl.render.Render3DEvent
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.mask.BoxOutlineMask
import dev.luna5ama.trollhack.graphics.mask.BoxVertexMask
import dev.luna5ama.trollhack.graphics.mask.SideMask
import dev.luna5ama.trollhack.graphics.matrix.scope
import dev.luna5ama.trollhack.graphics.matrix.translatef
import dev.luna5ama.trollhack.graphics.shader.Shader
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.utils.createDirectByteBuffer
import dev.luna5ama.trollhack.utils.threads.RenderThreadExecutor
import net.minecraft.world.phys.AABB
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL11.GL_LINES
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL11.GL_UNSIGNED_SHORT
import org.lwjgl.opengl.GL11.glDrawElements
import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_STREAM_DRAW
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL15.glDeleteBuffers
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL41.glProgramUniform1f
import org.lwjgl.opengl.GL41.glProgramUniformMatrix4fv
import org.lwjgl.opengl.GL45
import org.lwjgl.opengl.GL45.glCreateBuffers
import org.lwjgl.opengl.GL45.glCreateVertexArrays
import org.lwjgl.opengl.GL45.glEnableVertexArrayAttrib
import org.lwjgl.opengl.GL45.glNamedBufferData
import org.lwjgl.opengl.GL45.glVertexArrayAttribBinding
import org.lwjgl.opengl.GL45.glVertexArrayAttribFormat
import org.lwjgl.opengl.GL45.glVertexArrayElementBuffer
import org.lwjgl.opengl.GL45.glVertexArrayVertexBuffer
import java.nio.ByteBuffer
import java.nio.ShortBuffer

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

        RS.matrixLayer.scope {
            DrawShader.bind()
            glBindVertexArray(vaoID)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboID)

            translatef(posX.toFloat(), posY.toFloat(), posZ.toFloat())

            DrawShader.updateMatrix()
            if (filled) {
                DrawShader.alpha(filledAlpha)
                glDrawElements(GL_TRIANGLES, filledSize, GL_UNSIGNED_SHORT, 0L)
            }

            if (outline) {
                DrawShader.alpha(outlineAlpha)
                glDrawElements(GL_LINES, outlineSize, GL_UNSIGNED_SHORT, 2L * filledSize)
            }

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
            glBindVertexArray(0)
        }
    }

    inner class Builder {
        private val posList = FloatArrayList()
        private val intList = IntArrayList()
        private val filledIndexList = ShortArrayList()
        private val outlineIndexList = ShortArrayList()

        private val posX = 0.0
        private val posY = 0.0
        private val posZ = 0.0

        private var boxSize = 0
        private var vertexSize = 0

        fun putBox(box: AABB, color: ColorRGBA, sideMask: SideMask) {
            putBox(box, color, sideMask, sideMask.toOutlineMask())
        }

        fun putBox(box: AABB, color: ColorRGBA, sideMask: SideMask, outlineMask: BoxOutlineMask) {
            if (sideMask.mask or outlineMask.mask == 0) {
                return
            } else if (sideMask == SideMask.ALL && outlineMask == BoxOutlineMask.ALL) {
                putBox(box, color)
                return
            }

            val minX = box.minX.toFloat()
            val minY = box.minY.toFloat()
            val minZ = box.minZ.toFloat()
            val maxX = box.maxX.toFloat()
            val maxY = box.maxY.toFloat()
            val maxZ = box.maxZ.toFloat()

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

        fun putBox(box: AABB, color: ColorRGBA) {
            val minX = box.minX.toFloat()
            val minY = box.minY.toFloat()
            val minZ = box.minZ.toFloat()
            val maxX = box.maxX.toFloat()
            val maxY = box.maxY.toFloat()
            val maxZ = box.maxZ.toFloat()

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

            RenderThreadExecutor.execute {
                if (vaoID == 0) vaoID = glCreateVertexArrays()

                if (initialized) {
                    glDeleteBuffers(vboID)
                    glDeleteBuffers(iboID)
                }

                vboID = glCreateBuffers()
                iboID = glCreateBuffers()

                glVertexArrayVertexBuffer(vaoID, 0, vboID, 0, 16)
                glNamedBufferData(vboID, vboBuffer, GL_STREAM_DRAW)

                glEnableVertexArrayAttrib(vaoID, 0)
                glVertexArrayAttribBinding(vaoID, 0, 0)
                glVertexArrayAttribFormat(vaoID, 0, 3, GL_FLOAT, false, 0)

                glEnableVertexArrayAttrib(vaoID, 1)
                glVertexArrayAttribBinding(vaoID, 1, 0)
                glVertexArrayAttribFormat(vaoID, 1, 4, GL_UNSIGNED_BYTE, true, 12)

                glVertexArrayElementBuffer(vaoID, iboID)
                glNamedBufferData(iboID, iboBuffer, GL_STREAM_DRAW)

                this@StaticBoxRenderer.posX = posX
                this@StaticBoxRenderer.posY = posY
                this@StaticBoxRenderer.posZ = posZ

                this@StaticBoxRenderer.size = boxSize
                this@StaticBoxRenderer.filledSize = filledSize
                this@StaticBoxRenderer.outlineSize = outlineSize
            }
        }

        private fun buildVboBuffer(): ByteBuffer {
            val vboBuffer = createDirectByteBuffer(vertexSize * 16)

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
            val iboBuffer =
                createDirectByteBuffer((filledIndexList.size + outlineIndexList.size) * 2).asShortBuffer()

            iboBuffer.put(filledIndexList.elements(), 0, filledIndexList.size)
            iboBuffer.put(outlineIndexList.elements(), 0, outlineIndexList.size)

            iboBuffer.flip()
            return iboBuffer
        }
    }

    object DrawShader : Shader(
        "/assets/shader/general/StaticBoxRenderer.vsh",
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