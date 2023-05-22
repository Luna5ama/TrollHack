package dev.luna5ama.trollhack.util.graphics.font.glyph

import dev.fastmc.common.ParallelUtils
import dev.fastmc.common.UpdateCounter
import dev.fastmc.memutil.MemoryArray
import dev.luna5ama.trollhack.util.graphics.font.GlyphCache
import dev.luna5ama.trollhack.util.graphics.font.GlyphTexture
import dev.luna5ama.trollhack.util.graphics.font.Style
import dev.luna5ama.trollhack.util.graphics.glCompressedTextureSubImage2D
import dev.luna5ama.trollhack.util.graphics.texture.BC4Compression
import dev.luna5ama.trollhack.util.graphics.texture.Mipmaps
import dev.luna5ama.trollhack.util.graphics.texture.RawImage
import dev.luna5ama.trollhack.util.threads.BackgroundScope
import dev.luna5ama.trollhack.util.threads.onMainThreadSuspend
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.lwjgl.opengl.GL30.GL_COMPRESSED_RED_RGTC1
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import java.util.concurrent.atomic.AtomicIntegerArray

class FontGlyphs(val id: Int, private val font: Font, private val fallbackFont: Font, private val textureSize: Int) {
    private val chunkArray = arrayOfNulls<GlyphChunk>(128)
    private val loadingFlags = AtomicIntegerArray(127)
    private val mainChunk: GlyphChunk
    private val loadingLimiter = Semaphore(4)
    private val uploadMutex = Mutex()
    private val updateCounter = UpdateCounter()

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

        val texture = GlyphTexture(textureSize, textureSize, 4)

        runBlocking {
            val rawImage = withContext(Dispatchers.Default) {
                dumpAlphaParallel(textureImage)
            }
            val output = MemoryArray.malloc(BC4Compression.getEncodedSize(Mipmaps.getTotalSize(rawImage, 4)))

            coroutineScope {
                val mainThread = this
                withContext(Dispatchers.Default) {
                    var offset = 0L
                    Mipmaps.generate(rawImage, 4).withIndex().collect { (level, it) ->
                        val size = BC4Compression.getEncodedSize(it.data.size.toLong())
                        val i = offset
                        offset += size
                        launch {
                            val view = MemoryArray.wrap(output, i, size)
                            BC4Compression.encode(it, view)
                            mainThread.launch {
                                glCompressedTextureSubImage2D(
                                    texture.textureID,
                                    level,
                                    0,
                                    0,
                                    it.width,
                                    it.height,
                                    GL_COMPRESSED_RED_RGTC1,
                                    size.toInt(),
                                    view
                                )
                            }
                        }
                    }
                }
            }

            output.free()
        }

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

                if (loadingFlags.getAndSet(loadingID, 1) == 0) {
                    BackgroundScope.launch {
                        loadingLimiter.withPermit {
                            chunkArray[chunkID] = loadGlyphChunkAsync(chunkID)
                            updateCounter.update()
                        }
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

    fun checkUpdate(): Boolean {
        return updateCounter.check()
    }

    private suspend fun loadGlyphChunkAsync(chunkID: Int): GlyphChunk {
        return coroutineScope {

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

            val rawImage = dumpAlphaParallel(textureImage)
            val output = MemoryArray.malloc(BC4Compression.getEncodedSize(Mipmaps.getTotalSize(rawImage, 4)))

            val deferredTexture = onMainThreadSuspend {
                GlyphTexture(textureSize, textureSize, 4)
            }

            coroutineScope {
                var offset = 0L

                Mipmaps.generate(rawImage, 4).withIndex().collect { (level, it) ->
                    val size = BC4Compression.getEncodedSize(it.data.size.toLong())
                    val i = offset
                    offset += size
                    launch {
                        val view = MemoryArray.wrap(output, i, size)
                        BC4Compression.encode(it, view)
                        val texture = deferredTexture.await()
                        uploadSmoothed {
                            glCompressedTextureSubImage2D(
                                texture.textureID,
                                level,
                                0,
                                0,
                                it.width,
                                it.height,
                                GL_COMPRESSED_RED_RGTC1,
                                size.toInt(),
                                view
                            )
                        }
                    }
                }
            }

            output.free()

            GlyphChunk(chunkID, deferredTexture.await(), charInfoArray)
        }
    }

    private suspend fun uploadSmoothed(block: () -> Unit) {
        uploadMutex.withLock {
            onMainThreadSuspend(block).join()
        }
    }

    private fun drawGlyphs(bufferedImage: BufferedImage, chunkStart: Int): Array<CharInfo> {
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

    private suspend inline fun dumpAlphaParallel(image: BufferedImage): RawImage {
        val size = image.width * image.height
        val outputData = ByteArray(size)
        val isInt = image.raster.transferType == DataBuffer.TYPE_INT

        coroutineScope {
            if (isInt) {
                ParallelUtils.splitListIndex(size) { start, end ->
                    launch {
                        val data = IntArray(1)
                        for (i in start until end) {
                            image.raster.getDataElements(i % image.width, i / image.width, data)
                            outputData[i] = (data[0] shr 24).toByte()
                        }
                    }
                }
            } else {
                ParallelUtils.splitListIndex(size) { start, end ->
                    launch {
                        val data = ByteArray(4)
                        for (i in start until end) {
                            image.raster.getDataElements(i % image.width, i / image.width, data)
                            outputData[i] = data[3]
                        }
                    }
                }
            }
        }

        return RawImage(outputData, image.width, image.height, 1)
    }

}