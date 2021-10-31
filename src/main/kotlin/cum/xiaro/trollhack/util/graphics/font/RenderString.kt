package cum.xiaro.trollhack.util.graphics.font

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.util.graphics.font.glyph.CharInfo
import cum.xiaro.trollhack.util.graphics.font.glyph.GlyphChunk
import cum.xiaro.trollhack.util.graphics.font.renderer.AbstractFontRenderer
import cum.xiaro.trollhack.util.graphics.shaders.DrawShader
import cum.xiaro.trollhack.util.graphics.use
import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.client.renderer.GLAllocation
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer

class RenderString(private val string: CharSequence) {
    private val renderInfoList = ArrayList<StringRenderInfo>()

    private val initTime = System.currentTimeMillis()
    private var lastAccess = initTime

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

                renderInfo.put(posX, posY, charInfo, context.color)

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
            private val uvList = FloatArrayList()
            private val colorList = IntArrayList()

            fun put(posX: Float, posY: Float, charInfo: CharInfo, color: ColorRGB) {
                posList.add(posX)
                posList.add(posY)
                uvList.add(charInfo.u1)
                uvList.add(charInfo.v1)

                posList.add(posX + charInfo.renderWidth)
                posList.add(posY)
                uvList.add(charInfo.u2)
                uvList.add(charInfo.v1)

                posList.add(posX)
                posList.add(posY + charInfo.height)
                uvList.add(charInfo.u1)
                uvList.add(charInfo.v2)

                posList.add(posX + charInfo.renderWidth)
                posList.add(posY + charInfo.height)
                uvList.add(charInfo.u2)
                uvList.add(charInfo.v2)

                colorList.add(color.rgba)

                size++
            }

            fun build(): StringRenderInfo {
                val vboBuffer = buildVboBuffer()
                val iboBuffer = buildIboBuffer()

                val vaoID = glGenVertexArrays()
                val vboID = glGenBuffers()
                val iboID = glGenBuffers()

                glBindVertexArray(vaoID)

                glBindBuffer(GL_ARRAY_BUFFER, vboID)
                glBufferData(GL_ARRAY_BUFFER, vboBuffer, GL_STATIC_DRAW)

                glVertexAttribPointer(0, 2, GL_FLOAT, false, 20, 0L)
                glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, true, 20, 8L)
                glVertexAttribPointer(2, 2, GL_FLOAT, false, 20, 12L)

                glBindBuffer(GL_ARRAY_BUFFER, 0)

                glEnableVertexAttribArray(0)
                glEnableVertexAttribArray(1)
                glEnableVertexAttribArray(2)

                glBindVertexArray(0)

                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboID)
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, iboBuffer, GL_STATIC_DRAW)
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

                return StringRenderInfo(glyphChunk, size, vaoID, vboID, iboID)
            }

            private fun buildVboBuffer(): ByteBuffer {
                val vboBuffer = GLAllocation.createDirectByteBuffer(size * 4 * 2 * 20)

                var posIndex = 0
                var uvIndex = 0

                for (i in colorList.indices) {
                    val color = colorList.getInt(i)
                    val shadowColor = getShadowColor(ColorRGB(color)).rgba

                    vboBuffer.putFloat(posList.getFloat(posIndex++) + shadowDist)
                    vboBuffer.putFloat(posList.getFloat(posIndex++) + shadowDist)
                    vboBuffer.putInt(shadowColor)
                    vboBuffer.putFloat(uvList.getFloat(uvIndex++))
                    vboBuffer.putFloat(uvList.getFloat(uvIndex++))

                    vboBuffer.putFloat(posList.getFloat(posIndex++) + shadowDist)
                    vboBuffer.putFloat(posList.getFloat(posIndex++) + shadowDist)
                    vboBuffer.putInt(shadowColor)
                    vboBuffer.putFloat(uvList.getFloat(uvIndex++))
                    vboBuffer.putFloat(uvList.getFloat(uvIndex++))

                    vboBuffer.putFloat(posList.getFloat(posIndex++) + shadowDist)
                    vboBuffer.putFloat(posList.getFloat(posIndex++) + shadowDist)
                    vboBuffer.putInt(shadowColor)
                    vboBuffer.putFloat(uvList.getFloat(uvIndex++))
                    vboBuffer.putFloat(uvList.getFloat(uvIndex++))

                    vboBuffer.putFloat(posList.getFloat(posIndex++) + shadowDist)
                    vboBuffer.putFloat(posList.getFloat(posIndex++) + shadowDist)
                    vboBuffer.putInt(shadowColor)
                    vboBuffer.putFloat(uvList.getFloat(uvIndex++))
                    vboBuffer.putFloat(uvList.getFloat(uvIndex++))
                }

                posIndex = 0
                uvIndex = 0

                for (i in colorList.indices) {
                    val color = colorList.getInt(i)

                    vboBuffer.putFloat(posList.getFloat(posIndex++))
                    vboBuffer.putFloat(posList.getFloat(posIndex++))
                    vboBuffer.putInt(color)
                    vboBuffer.putFloat(uvList.getFloat(uvIndex++))
                    vboBuffer.putFloat(uvList.getFloat(uvIndex++))

                    vboBuffer.putFloat(posList.getFloat(posIndex++))
                    vboBuffer.putFloat(posList.getFloat(posIndex++))
                    vboBuffer.putInt(color)
                    vboBuffer.putFloat(uvList.getFloat(uvIndex++))
                    vboBuffer.putFloat(uvList.getFloat(uvIndex++))

                    vboBuffer.putFloat(posList.getFloat(posIndex++))
                    vboBuffer.putFloat(posList.getFloat(posIndex++))
                    vboBuffer.putInt(color)
                    vboBuffer.putFloat(uvList.getFloat(uvIndex++))
                    vboBuffer.putFloat(uvList.getFloat(uvIndex++))

                    vboBuffer.putFloat(posList.getFloat(posIndex++))
                    vboBuffer.putFloat(posList.getFloat(posIndex++))
                    vboBuffer.putInt(color)
                    vboBuffer.putFloat(uvList.getFloat(uvIndex++))
                    vboBuffer.putFloat(uvList.getFloat(uvIndex++))
                }

                vboBuffer.flip()
                return vboBuffer
            }

            private fun buildIboBuffer(): ByteBuffer {
                val iboBuffer = GLAllocation.createDirectByteBuffer(size * 2 * 6 * 2)

                val indexSize = size * 2 * 4
                var index = 0
                while (index < indexSize) {
                    iboBuffer.putShort(index.toShort())
                    iboBuffer.putShort((index + 2).toShort())
                    iboBuffer.putShort((index + 1).toShort())
                    iboBuffer.putShort((index + 3).toShort())
                    iboBuffer.putShort((index + 1).toShort())
                    iboBuffer.putShort((index + 2).toShort())
                    index += 4
                }

                iboBuffer.flip()
                return iboBuffer
            }

            private fun getShadowColor(color: ColorRGB): ColorRGB {
                return ColorRGB(color.r / 5, color.g / 5, color.b / 5, color.a * 8 / 10)
            }
        }
    }
}