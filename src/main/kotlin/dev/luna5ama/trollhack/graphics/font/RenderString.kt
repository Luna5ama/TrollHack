package dev.luna5ama.trollhack.graphics.font

import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.asMutable
import dev.luna5ama.kmogus.ensureCapacity
import dev.luna5ama.trollhack.graphics.GLDataType
import dev.luna5ama.trollhack.graphics.buildAttribute
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.font.glyph.CharInfo
import dev.luna5ama.trollhack.graphics.font.glyph.GlyphChunk
import dev.luna5ama.trollhack.graphics.font.renderer.AbstractFontRenderContext
import dev.luna5ama.trollhack.graphics.font.renderer.AbstractFontRenderer
import dev.luna5ama.trollhack.graphics.glNamedBufferStorage
import dev.luna5ama.trollhack.graphics.shaders.DrawShader
import dev.luna5ama.trollhack.graphics.use
import dev.luna5ama.trollhack.structs.FontVertex
import dev.luna5ama.trollhack.structs.sizeof
import dev.luna5ama.trollhack.util.collections.forEachFast
import net.minecraft.client.renderer.GlStateManager
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import org.lwjgl.opengl.GL11.GL_UNSIGNED_SHORT
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glDeleteVertexArrays
import org.lwjgl.opengl.GL45.glCreateBuffers
import org.lwjgl.opengl.GL45.glCreateVertexArrays

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

        renderInfoList.forEachFast {
            it.render(drawShadow, lodBias)
        }

        glBindVertexArray(0)
        GlStateManager.bindTexture(0)
    }

    fun tryClean(current: Long): Boolean {
        return if (invalid || current - initTime >= 15000L || current - lastAccess >= 5000L) {
            destroy()
            true
        } else {
            false
        }
    }

    fun destroy() {
        renderInfoList.forEachFast {
            it.destroy()
        }
        renderInfoList.clear()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RenderString

        return string == other.string
    }

    override fun hashCode(): Int {
        return string.hashCode()
    }

    private object Shader : DrawShader(
        "/assets/trollhack/shaders/general/FontRenderer.vsh",
        "/assets/trollhack/shaders/general/FontRenderer.fsh"
    ) {
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

    private class StringRenderInfo private constructor(
        private val glyphChunk: GlyphChunk,
        private val size: Int,
        private val vaoID: Int,
        private val vboID: Int,
        private val iboID: Int
    ) {
        fun render(drawShadow: Boolean, lodBias: Float) {
            glyphChunk.texture.bindTexture()
            glyphChunk.updateLodBias(lodBias)

            glBindVertexArray(vaoID)

            if (drawShadow) {
                dev.luna5ama.trollhack.graphics.glDrawElements(GL_TRIANGLES, size * 2 * 6, GL_UNSIGNED_SHORT, 0L)
            } else {
                dev.luna5ama.trollhack.graphics.glDrawElements(
                    GL_TRIANGLES,
                    size * 6,
                    GL_UNSIGNED_SHORT,
                    size * 6L * 2L
                )
            }
        }

        fun destroy() {
            glDeleteVertexArrays(vaoID)
            glDeleteBuffers(vboID)
            glDeleteBuffers(iboID)
        }

        class Builder(private val glyphChunk: GlyphChunk, private val shadowDist: Float) {
            private var size = 0

            private val array = Arr.malloc(16L * 4L * 2L).asMutable()

            fun put(posX: Float, posY: Float, charInfo: CharInfo, context: AbstractFontRenderContext) {
                val color = (context.color + 1).toByte()
                val overrideColor = (if (color.toInt() == 0) 1 else 0).toByte()

                var pX = posX
                var pY = posY
                var u = charInfo.uv[0]
                var v = charInfo.uv[1]

                array.ensureCapacity(++size * 16L * 4L * 2L, false)

                var struct = FontVertex(array)

                struct.position.x = pX + shadowDist
                struct.position.y = pY + shadowDist
                struct.vertUV.x = u
                struct.vertUV.y = v
                struct.colorIndex = color
                struct.overrideColor = overrideColor
                struct.shadow = 1
                struct++

                struct.position.x = pX
                struct.position.y = pY
                struct.vertUV.x = u
                struct.vertUV.y = v
                struct.colorIndex = color
                struct.overrideColor = overrideColor
                struct.shadow = 0
                struct++

                pX = posX + charInfo.renderWidth
                pY = posY
                u = charInfo.uv[2]
                v = charInfo.uv[1]

                struct.position.x = pX + shadowDist
                struct.position.y = pY + shadowDist
                struct.vertUV.x = u
                struct.vertUV.y = v
                struct.colorIndex = color
                struct.overrideColor = overrideColor
                struct.shadow = 1
                struct++

                struct.position.x = pX
                struct.position.y = pY
                struct.vertUV.x = u
                struct.vertUV.y = v
                struct.colorIndex = color
                struct.overrideColor = overrideColor
                struct.shadow = 0
                struct++

                pX = posX
                pY = posY + charInfo.height
                u = charInfo.uv[0]
                v = charInfo.uv[3]

                struct.position.x = pX + shadowDist
                struct.position.y = pY + shadowDist
                struct.vertUV.x = u
                struct.vertUV.y = v
                struct.colorIndex = color
                struct.overrideColor = overrideColor
                struct.shadow = 1
                struct++

                struct.position.x = pX
                struct.position.y = pY
                struct.vertUV.x = u
                struct.vertUV.y = v
                struct.colorIndex = color
                struct.overrideColor = overrideColor
                struct.shadow = 0
                struct++

                pX = posX + charInfo.renderWidth
                pY = posY + charInfo.height
                u = charInfo.uv[2]
                v = charInfo.uv[3]

                struct.position.x = pX + shadowDist
                struct.position.y = pY + shadowDist
                struct.vertUV.x = u
                struct.vertUV.y = v
                struct.colorIndex = color
                struct.overrideColor = overrideColor
                struct.shadow = 1
                struct++

                struct.position.x = pX
                struct.position.y = pY
                struct.vertUV.x = u
                struct.vertUV.y = v
                struct.colorIndex = color
                struct.overrideColor = overrideColor
                struct.shadow = 0
                struct++

                array.pos(struct.ptr)
            }

            fun build(): StringRenderInfo {
                val vaoID = glCreateVertexArrays()
                val vboID = glCreateBuffers()
                val iboID = glCreateBuffers()

                glNamedBufferStorage(vboID, size * 16L * 4L * 2L, array.basePtr, 0)

                array.reset()
                buildIboBuffer()
                glNamedBufferStorage(iboID, size * 2L * 6L * 2L, array.basePtr, 0)
                array.free()

                glBindVertexArray(vaoID)
                glBindBuffer(GL_ARRAY_BUFFER, vboID)
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboID)
                vertexAttribute.apply()

                glBindVertexArray(0)
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
                glBindBuffer(GL_ARRAY_BUFFER, 0)

                return StringRenderInfo(glyphChunk, size, vaoID, vboID, iboID)
            }

            private fun buildIboBuffer() {
                val indexSize = size * 2 * 4
                var index = 0
                var pointer = array.ptr

                while (index < indexSize) {
                    pointer.setShort(index.toShort())
                    pointer += 2
                    pointer.setShort((index + 4).toShort())
                    pointer += 2
                    pointer.setShort((index + 2).toShort())
                    pointer += 2
                    pointer.setShort((index + 6).toShort())
                    pointer += 2
                    pointer.setShort((index + 2).toShort())
                    pointer += 2
                    pointer.setShort((index + 4).toShort())
                    pointer += 2
                    index += 8
                }

                index = 0

                while (index < indexSize) {
                    pointer.setShort((index + 1).toShort())
                    pointer += 2
                    pointer.setShort((index + 5).toShort())
                    pointer += 2
                    pointer.setShort((index + 3).toShort())
                    pointer += 2
                    pointer.setShort((index + 7).toShort())
                    pointer += 2
                    pointer.setShort((index + 3).toShort())
                    pointer += 2
                    pointer.setShort((index + 5).toShort())
                    pointer += 2
                    index += 8
                }
            }

            private companion object {
                val vertexAttribute = buildAttribute(sizeof(FontVertex).toInt()) {
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