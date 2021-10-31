package cum.xiaro.trollhack.util.graphics.font.glyph

import cum.xiaro.trollhack.util.graphics.font.Style
import cum.xiaro.trollhack.util.graphics.texture.GrayscaleMipmapTexture
import cum.xiaro.trollhack.util.graphics.texture.TextureUtils.getAlpha
import cum.xiaro.trollhack.util.threads.TrollHackScope
import cum.xiaro.trollhack.util.threads.onMainThreadSuspend
import kotlinx.coroutines.launch
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage

class FontGlyphs(val id: Int, private val font: Font, private val fallbackFont: Font, private val textureSize: Int) {
    private val chunkArray = arrayOfNulls<GlyphChunk>(128)
    private val loadingArray = BooleanArray(127)
    private val mainChunk: GlyphChunk

    init {
        val textureImage = BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB)
        val graphics2D = textureImage.createGraphics()
        val charInfoArray = drawGlyphs(graphics2D, 0)
        val data = textureImage.getAlpha()
        val texture = createTexture(data, textureImage.width, textureImage.height)

        mainChunk = GlyphChunk(0, texture, charInfoArray)
        chunkArray[0] = mainChunk
    }

    val fontHeight = (mainChunk.charInfoArray.maxOfOrNull { it.height } ?: font.size).toFloat()

    /** @return CharInfo of [char] */
    fun getCharInfo(char: Char): CharInfo {
        val charInt = char.code
        val chunk = charInt shr 9
        val chunkStart = chunk shl 9
        return getChunk(chunk).charInfoArray[charInt - chunkStart]
    }

    /** @return the chunk the [char] is in */
    fun getChunk(char: Char): GlyphChunk {
        return getChunk(char.code shr 9)
    }

    /** @return the chunk */
    fun getChunk(chunkID: Int): GlyphChunk {
        return if (chunkID !in chunkArray.indices) {
            mainChunk
        } else {
            var chunk = chunkArray[chunkID]

            if (chunk == null) {
                chunk = mainChunk
                val loadingID = chunkID - 1
                val flag: Boolean

                synchronized(loadingArray) {
                    flag = !loadingArray[loadingID]
                    loadingArray[loadingID] = true
                }

                if (flag) {
                    TrollHackScope.launch {
                        chunkArray[chunkID] = loadGlyphChunkAsync(chunkID)
                    }
                }
            }

            chunk
        }
    }

    /** Delete all textures */
    fun destroy() {
        for (i in chunkArray.indices) {
            val chunk = chunkArray[i]
            chunkArray[i] = null
            chunk?.texture?.deleteTexture()
        }
    }

    private suspend fun loadGlyphChunkAsync(chunkID: Int): GlyphChunk {
        val textureImage = BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB)
        val graphics2D = textureImage.createGraphics()
        val charInfoArray = drawGlyphs(graphics2D, chunkID shl 9)
        val data = textureImage.getAlpha()
        val texture = onMainThreadSuspend { createTexture(data, textureImage.width, textureImage.height) }.await()
        return GlyphChunk(chunkID, texture, charInfoArray)
    }

    private fun drawGlyphs(graphics2D: Graphics2D, chunkStart: Int): Array<CharInfo> {
        graphics2D.background = Color(0, 0, 0, 0)
        graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        var rowHeight = 0
        var positionX = 0
        var positionY = 0
        val textureSizeFloat = textureSize.toFloat()
        val xPadding = if (id == Style.ITALIC.ordinal) 12 else 16
        val extraWidth = if (id == Style.ITALIC.ordinal) 4.0f else 0.0f

        val charInfoArray = Array(512) { index ->
            val char = (chunkStart + index).toChar() // Plus 1 because char starts at 1 not 0

            graphics2D.font = if (font.canDisplay(char.code)) font else fallbackFont

            val fontMetrics = graphics2D.fontMetrics
            val width = fontMetrics.charWidth(char).let { if (it > 0) it else 8 }
            val height = fontMetrics.height.let { if (it > 0) it else font.size }

            // Move to the next line if we reach the texture width
            if (positionX + width >= textureSize) {
                positionX = 1
                positionY += rowHeight + 8
                rowHeight = 0
            }

            graphics2D.drawString(char.toString(), positionX, positionY + fontMetrics.ascent)
            val charInfo = CharInfo(textureSizeFloat, width, height, positionX, positionY, extraWidth)

            if (height > rowHeight) rowHeight = height // Update row height
            positionX += width + xPadding // Move right for next character

            charInfo
        }

        graphics2D.dispose()
        return charInfoArray
    }

    private fun createTexture(byteArray: ByteArray, width: Int, height: Int): GrayscaleMipmapTexture {
        return GrayscaleMipmapTexture(byteArray, width, height, 4).apply {
            bindTexture()

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0.0f)

            unbindTexture()
        }
    }
}