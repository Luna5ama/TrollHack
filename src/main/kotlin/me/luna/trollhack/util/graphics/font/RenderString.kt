package me.luna.trollhack.util.graphics.font

import it.unimi.dsi.fastutil.bytes.ByteArrayList
import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.shorts.ShortArrayList
import me.luna.trollhack.util.graphics.GLDataType
import me.luna.trollhack.util.graphics.buildAttribute
import me.luna.trollhack.util.graphics.color.ColorRGB
import me.luna.trollhack.util.graphics.font.glyph.CharInfo
import me.luna.trollhack.util.graphics.font.glyph.GlyphChunk
import me.luna.trollhack.util.graphics.font.renderer.AbstractFontRenderContext
import me.luna.trollhack.util.graphics.font.renderer.AbstractFontRenderer
import me.luna.trollhack.util.graphics.shaders.DrawShader
import me.luna.trollhack.util.graphics.use
import me.luna.trollhack.util.skip
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer

class RenderString(fontRenderer: AbstractFontRenderer, private val string: CharSequence) {
    private val renderInfoList = ArrayList<StringRenderInfo>()

    private val initTime = System.currentTimeMillis()
    private var lastAccess = initTime
    val width: Float

    init {
        var maxLineWidth = 0.0f
        var width = 0.0f
        val context = fontRenderer.renderContext

        for ((index, char) in string.withIndex()) {
            if (char == '\n') {
                if (width > maxLineWidth) maxLineWidth = width
                width = 0.0f
            }
            if (context.checkFormatCode(string, index, false)) continue
            width += fontRenderer.regularGlyph.getCharInfo(char).width + fontRenderer.charGap
        }

        this.width = width
    }

    var invalid = false; private set

    fun build(fontRenderer: AbstractFontRenderer, charGap: Float, lineSpace: Float, shadowDist: Float): RenderString {
        val builderArray = Array(3) {
            arrayOfNulls<StringRenderInfo.Builder>(128)
        }

        var posX = 0.0f
        var posY = 0.0f

        val context = fontRenderer.renderContext

        for ((index, char) in string.withIndex()) {
            if (context.checkFormatCode(string, index, true)) continue

            if (char == '\n') {
                posY += context.variant.fontHeight * lineSpace
                posX = 0.0f
            } else {
                val chunkID = char.code shr 9
                val chunk = context.variant.getChunk(chunkID)
                invalid = invalid || chunk.id != chunkID

                val charInt = char.code
                val charInfo = chunk.charInfoArray[charInt - (charInt shr 9 shl 9)]

                val variantArray = builderArray[context.variant.id]
                var renderInfo = variantArray[chunk.id]
                if (renderInfo == null) {
                    renderInfo = StringRenderInfo.Builder(chunk, shadowDist)
                    variantArray[chunk.id] = renderInfo
                }

                renderInfo.put(posX, posY, charInfo, context)

                posX += charInfo.width + charGap
            }
        }

        builderArray.forEach { array ->
            array.forEach {
                if (it != null) {
                    renderInfoList.add(it.build())
                }
            }
        }

        return this
    }

    fun render(modelView: Matrix4f, color: ColorRGB, drawShadow: Boolean, lodBias: Float) {
        lastAccess = System.currentTimeMillis()

        Shader.bind()
        Shader.preRender(modelView, color)

        renderInfoList.forEach {
            it.render(drawShadow, lodBias)
        }

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        glBindVertexArray(0)
    }

    fun tryClean(current: Long): Boolean {
        return if (current - initTime >= 15000L || invalid && current - initTime >= 1000L || current - lastAccess >= 15000L) {
            destroy()
            true
        } else {
            false
        }
    }

    fun destroy() {
        renderInfoList.forEach {
            it.destroy()
        }
        renderInfoList.clear()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RenderString

        if (string != other.string) return false

        return true
    }

    override fun hashCode(): Int {
        return string.hashCode()
    }

    private object Shader : DrawShader("/assets/trollhack/shaders/general/FontRenderer.vsh", "/assets/trollhack/shaders/general/FontRenderer.fsh") {
        val defaultColorUniform = glGetUniformLocation(id, "defaultColor")

        init {
            use {
                glUniform1i(glGetUniformLocation(id, "texture"), 0)
            }
        }

        fun preRender(modelView: Matrix4f, color: ColorRGB) {
            updateProjectionMatrix()
            updateModelViewMatrix(modelView)
            glUniform4f(defaultColorUniform, color.rFloat, color.gFloat, color.bFloat, color.aFloat)
        }
    }

    private class StringRenderInfo private constructor(private val glyphChunk: GlyphChunk, private val size: Int, private val vaoID: Int, private val vboID: Int, private val iboID: Int) {
        fun render(drawShadow: Boolean, lodBias: Float) {
            glyphChunk.texture.bindTexture()
            glyphChunk.updateLodBias(lodBias)

            glBindVertexArray(vaoID)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboID)

            if (drawShadow) {
                glDrawElements(GL_TRIANGLES, size * 2 * 6, GL_UNSIGNED_SHORT, 0L)
            } else {
                glDrawElements(GL_TRIANGLES, size * 6, GL_UNSIGNED_SHORT, size * 6L * 2L)
            }
        }

        fun destroy() {
            glDeleteVertexArrays(vaoID)
            glDeleteBuffers(vboID)
            glDeleteBuffers(iboID)
        }

        class Builder(private val glyphChunk: GlyphChunk, private val shadowDist: Float) {
            private var size = 0

            private val posList = FloatArrayList()
            private val uvList = ShortArrayList()
            private val colorList = ByteArrayList()

            fun put(posX: Float, posY: Float, charInfo: CharInfo, context: AbstractFontRenderContext) {
                val color = context.color + 1

                posList.add(posX)
                posList.add(posY)
                uvList.add(charInfo.uv[0])
                uvList.add(charInfo.uv[1])

                posList.add(posX + charInfo.renderWidth)
                posList.add(posY)
                uvList.add(charInfo.uv[2])
                uvList.add(charInfo.uv[1])

                posList.add(posX)
                posList.add(posY + charInfo.height)
                uvList.add(charInfo.uv[0])
                uvList.add(charInfo.uv[3])

                posList.add(posX + charInfo.renderWidth)
                posList.add(posY + charInfo.height)
                uvList.add(charInfo.uv[2])
                uvList.add(charInfo.uv[3])

                colorList.add(color.toByte())

                size++
            }

            fun build(): StringRenderInfo {
                val vaoID = glGenVertexArrays()
                val vboID = glGenBuffers()
                val iboID = glGenBuffers()

                glBindVertexArray(vaoID)

                glBindBuffer(GL_ARRAY_BUFFER, vboID)
                glBufferData(GL_ARRAY_BUFFER, size * 16L * 4L * 2L, GL_STATIC_DRAW)
                val vboBuffer = glMapBuffer(GL_ARRAY_BUFFER, GL_WRITE_ONLY, null)
                buildVboBuffer(vboBuffer)
                glUnmapBuffer(GL_ARRAY_BUFFER)

                vertexAttribute.apply()
                glBindBuffer(GL_ARRAY_BUFFER, 0)
                glBindVertexArray(0)

                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboID)
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, size * 2L * 6L * 2L, GL_STATIC_DRAW)
                val iboBuffer = glMapBuffer(GL_ELEMENT_ARRAY_BUFFER, GL_WRITE_ONLY, null)
                buildIboBuffer(iboBuffer)
                glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER)
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

                return StringRenderInfo(glyphChunk, size, vaoID, vboID, iboID)
            }

            private fun buildVboBuffer(buffer: ByteBuffer): ByteBuffer {
                var posIndex = 0
                var uvIndex = 0

                for (i in colorList.indices) {
                    val color = colorList.getByte(i)
                    val overrideColor = (if (color.toInt() == 0) 1 else 0).toByte()

                    var posX = posList.getFloat(posIndex++)
                    var posY = posList.getFloat(posIndex++)
                    var u = uvList.getShort(uvIndex++)
                    var v = uvList.getShort(uvIndex++)

                    buffer.putFloat(posX + shadowDist)
                    buffer.putFloat(posY + shadowDist)
                    buffer.putShort(u)
                    buffer.putShort(v)
                    buffer.put(color)
                    buffer.put(overrideColor)
                    buffer.put(1)
                    buffer.skip(1)

                    buffer.putFloat(posX)
                    buffer.putFloat(posY)
                    buffer.putShort(u)
                    buffer.putShort(v)
                    buffer.put(color)
                    buffer.put(overrideColor)
                    buffer.put(0)
                    buffer.skip(1)

                    posX = posList.getFloat(posIndex++)
                    posY = posList.getFloat(posIndex++)
                    u = uvList.getShort(uvIndex++)
                    v = uvList.getShort(uvIndex++)

                    buffer.putFloat(posX + shadowDist)
                    buffer.putFloat(posY + shadowDist)
                    buffer.putShort(u)
                    buffer.putShort(v)
                    buffer.put(color)
                    buffer.put(overrideColor)
                    buffer.put(1)
                    buffer.skip(1)

                    buffer.putFloat(posX)
                    buffer.putFloat(posY)
                    buffer.putShort(u)
                    buffer.putShort(v)
                    buffer.put(color)
                    buffer.put(overrideColor)
                    buffer.put(0)
                    buffer.skip(1)

                    posX = posList.getFloat(posIndex++)
                    posY = posList.getFloat(posIndex++)
                    u = uvList.getShort(uvIndex++)
                    v = uvList.getShort(uvIndex++)

                    buffer.putFloat(posX + shadowDist)
                    buffer.putFloat(posY + shadowDist)
                    buffer.putShort(u)
                    buffer.putShort(v)
                    buffer.put(color)
                    buffer.put(overrideColor)
                    buffer.put(1)
                    buffer.skip(1)

                    buffer.putFloat(posX)
                    buffer.putFloat(posY)
                    buffer.putShort(u)
                    buffer.putShort(v)
                    buffer.put(color)
                    buffer.put(overrideColor)
                    buffer.put(0)
                    buffer.skip(1)

                    posX = posList.getFloat(posIndex++)
                    posY = posList.getFloat(posIndex++)
                    u = uvList.getShort(uvIndex++)
                    v = uvList.getShort(uvIndex++)

                    buffer.putFloat(posX + shadowDist)
                    buffer.putFloat(posY + shadowDist)
                    buffer.putShort(u)
                    buffer.putShort(v)
                    buffer.put(color)
                    buffer.put(overrideColor)
                    buffer.put(1)
                    buffer.skip(1)

                    buffer.putFloat(posX)
                    buffer.putFloat(posY)
                    buffer.putShort(u)
                    buffer.putShort(v)
                    buffer.put(color)
                    buffer.put(overrideColor)
                    buffer.put(0)
                    buffer.skip(1)
                }

                buffer.flip()
                return buffer
            }

            private fun buildIboBuffer(buffer: ByteBuffer): ByteBuffer {
                val indexSize = size * 2 * 4
                var index = 0

                while (index < indexSize) {
                    buffer.putShort(index.toShort())
                    buffer.putShort((index + 4).toShort())
                    buffer.putShort((index + 2).toShort())
                    buffer.putShort((index + 6).toShort())
                    buffer.putShort((index + 2).toShort())
                    buffer.putShort((index + 4).toShort())
                    index += 8
                }

                index = 0

                while (index < indexSize) {
                    buffer.putShort((index + 1).toShort())
                    buffer.putShort((index + 5).toShort())
                    buffer.putShort((index + 3).toShort())
                    buffer.putShort((index + 7).toShort())
                    buffer.putShort((index + 3).toShort())
                    buffer.putShort((index + 5).toShort())
                    index += 8
                }

                buffer.flip()
                return buffer
            }

            private companion object {
                val vertexAttribute = buildAttribute(16) {
                    float(0, 2, GLDataType.GL_FLOAT, false)
                    float(1, 2, GLDataType.GL_UNSIGNED_SHORT, true)
                    int(2, 1, GLDataType.GL_BYTE)
                    float(3, 1, GLDataType.GL_UNSIGNED_BYTE, false)
                    float(4, 1, GLDataType.GL_UNSIGNED_BYTE, false)
                }
            }
        }
    }
}