package me.luna.trollhack.util.graphics.font.glyph

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.luna.trollhack.util.extension.ceilToInt
import me.luna.trollhack.util.graphics.font.GlyphCache
import me.luna.trollhack.util.graphics.font.GlyphTexture
import me.luna.trollhack.util.graphics.font.Style
import me.luna.trollhack.util.threads.TrollHackBackgroundScope
import me.luna.trollhack.util.threads.onMainThreadSuspend
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("NOTHING_TO_INLINE")
class FontGlyphs(val id: Int, private val font: Font, private val fallbackFont: Font, private val textureSize: Int) {
    private val chunkArray = arrayOfNulls<GlyphChunk>(128)
    private val loadingArray = Array<AtomicBoolean>(127) { AtomicBoolean(false) }
    private val mainChunk: GlyphChunk
    private val mutex = Mutex()

    init {
        val textureImage: BufferedImage
        val charInfoArray: Array<CharInfo>

        val cache = GlyphCache.get(font, 0)
        if (cache != null) {
            textureImage = cache.first
            charInfoArray = cache.second
        } else {
            textureImage = BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB)
            charInfoArray = drawGlyphs(textureImage, 0)
            GlyphCache.put(font, 0, textureImage, charInfoArray)
        }

        val data = runBlocking { getAlphaParallel(textureImage) }
        val texture = GlyphTexture(data, textureImage.width, textureImage.height, 4)

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

                val boolean = loadingArray[loadingID]

                if (!boolean.getAndSet(true)) {
                    TrollHackBackgroundScope.launch {
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
        val textureImage: BufferedImage
        val charInfoArray: Array<CharInfo>

        val cache = GlyphCache.get(font, chunkID)
        if (cache != null) {
            textureImage = cache.first
            charInfoArray = cache.second
        } else {
            textureImage = BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB)
            charInfoArray = drawGlyphs(textureImage, chunkID shl 9)
            GlyphCache.put(font, chunkID, textureImage, charInfoArray)
        }

        val data = getAlphaParallel(textureImage)
        val texture = mutex.withLock { onMainThreadSuspend { GlyphTexture(data, textureImage.width, textureImage.height, 4) }.await() }

        return GlyphChunk(chunkID, texture, charInfoArray)
    }

    private inline fun drawGlyphs(bufferedImage: BufferedImage, chunkStart: Int): Array<CharInfo> {
        val graphics2D = bufferedImage.createGraphics()

        graphics2D.background = Color(0, 0, 0, 0)
        graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

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

    private suspend inline fun getAlphaParallel(image: BufferedImage): ByteBuffer {
        val size = image.width * image.height
        val buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        val isInt = image.raster.transferType == DataBuffer.TYPE_INT

        coroutineScope {
            val cpus = Runtime.getRuntime().availableProcessors()
            val regionSize = (size.toDouble() / cpus.toDouble()).ceilToInt()
            var start = 0

            if (isInt) {
                while (start + regionSize < size) {
                    val regionStart = start

                    launch {
                        val end = regionStart + regionSize
                        val data = IntArray(1)
                        for (i in regionStart until end) {
                            image.raster.getDataElements(i % image.width, i / image.width, data)
                            buffer.put(i, (data[0] shr 24).toByte())
                        }
                    }

                    start += regionSize
                }

                val data = IntArray(1)
                for (i in start until size) {
                    image.raster.getDataElements(i % image.width, i / image.width, data)
                    buffer.put(i, (data[0] shr 24).toByte())
                }
            } else {
                while (start + regionSize < size) {
                    val regionStart = start

                    launch {
                        val end = regionStart + regionSize
                        val data = ByteArray(4)
                        for (i in regionStart until end) {
                            image.raster.getDataElements(i % image.width, i / image.width, data)
                            buffer.put(i, data[3])
                        }
                    }

                    start += regionSize
                }

                val data = ByteArray(4)
                for (i in start until size) {
                    image.raster.getDataElements(i % image.width, i / image.width, data)
                    buffer.put(i, data[3])
                }
            }
        }

        buffer.flip()
        return buffer
    }

}