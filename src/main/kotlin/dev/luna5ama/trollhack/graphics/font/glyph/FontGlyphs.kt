package dev.luna5ama.trollhack.graphics.font.glyph

import dev.fastmc.common.ParallelUtils
import dev.fastmc.common.UpdateCounter
import dev.luna5ama.kmogus.byteLength
import dev.luna5ama.kmogus.memcpy
import dev.luna5ama.trollhack.graphics.font.GlyphCache
import dev.luna5ama.trollhack.graphics.font.GlyphTexture
import dev.luna5ama.trollhack.graphics.font.Style
import dev.luna5ama.trollhack.graphics.glMapNamedBufferRange
import dev.luna5ama.trollhack.graphics.texture.BC4Compression
import dev.luna5ama.trollhack.graphics.texture.Mipmaps
import dev.luna5ama.trollhack.graphics.texture.RawImage
import dev.luna5ama.trollhack.util.threads.BackgroundScope
import dev.luna5ama.trollhack.util.threads.DefaultScope
import dev.luna5ama.trollhack.util.threads.onMainThread
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import net.minecraft.client.renderer.OpenGlHelper.glBindBuffer
import org.lwjgl.opengl.GL15.glDeleteBuffers
import org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER
import org.lwjgl.opengl.GL30.GL_COMPRESSED_RED_RGTC1
import org.lwjgl.opengl.GL30.GL_MAP_WRITE_BIT
import org.lwjgl.opengl.GL45.*
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicIntegerArray

class FontGlyphs(val id: Int, private val font: Font, private val fallbackFont: Font, private val textureSize: Int) {
    private val chunkArray = arrayOfNulls<GlyphChunk>(128)
    private val loadingFlags = AtomicIntegerArray(127)
    private val mainChunk: GlyphChunk
    private val loadingLimiter = Semaphore(4)
    private val uploadMutex = Mutex()
    private val updateCounter = UpdateCounter()

    init {
        val texture = GlyphTexture(textureSize, textureSize, 4)
        val glyphChunk: GlyphChunk

        val cache = runBlocking {
            withContext(DefaultScope.context) {
                GlyphCache.get(font, 0) ?: createNewGlyph(0)
            }
        }

        val bufferID = glCreateBuffers()
        glNamedBufferStorage(bufferID, cache.data.size.toLong(), GL_MAP_WRITE_BIT)
        val buffer = glMapNamedBufferRange(bufferID, 0, cache.data.size.toLong(), GL_MAP_WRITE_BIT)
        memcpy(cache.data, buffer.ptr, cache.data.byteLength)
        glUnmapNamedBuffer(bufferID)

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, bufferID)

        var offset = 0L
        var size = cache.baseSize
        for (i in 0..cache.levels) {
            val a = offset
            val s = size

            offset += size
            size = size shr 2

            glCompressedTextureSubImage2D(
                texture.textureID,
                i,
                0,
                0,
                cache.width shr i,
                cache.height shr i,
                GL_COMPRESSED_RED_RGTC1,
                s,
                a
            )
        }

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0)
        glDeleteBuffers(bufferID)

        glyphChunk = GlyphChunk(0, texture, cache.charInfoArray)

        mainChunk = glyphChunk
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
            val deferredTexture = onMainThread {
                GlyphTexture(textureSize, textureSize, 4)
            }

            val cache = GlyphCache.get(font, chunkID) ?: createNewGlyph(chunkID)

            val deferredBuffer = onMainThread {
                val bufferID = glCreateBuffers()
                glNamedBufferStorage(bufferID, cache.data.size.toLong(), GL_MAP_WRITE_BIT)
                bufferID to glMapNamedBufferRange(bufferID, 0, cache.data.size.toLong(), GL_MAP_WRITE_BIT)
            }

            val (bufferID, buffer) = deferredBuffer.await()
            memcpy(cache.data, buffer.ptr, cache.data.byteLength)
            onMainThread {
                glUnmapNamedBuffer(bufferID)
            }.await()

            val texture = deferredTexture.await()

            var offset = 0L
            var size = cache.baseSize
            for (i in 0..cache.levels) {
                val a = offset
                val s = size

                offset += size
                size = size shr 2
                uploadSmoothed {
                    glBindBuffer(GL_PIXEL_UNPACK_BUFFER, bufferID)
                    glCompressedTextureSubImage2D(
                        texture.textureID,
                        i,
                        0,
                        0,
                        cache.width shr i,
                        cache.height shr i,
                        GL_COMPRESSED_RED_RGTC1,
                        s,
                        a
                    )
                    glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0)
                }
            }

            onMainThread {
                glDeleteBuffers(bufferID)
            }

            GlyphChunk(chunkID, texture, cache.charInfoArray)
        }
    }

    private suspend fun uploadSmoothed(block: () -> Unit) {
        uploadMutex.withLock {
            onMainThread(block).join()
        }
    }

    private suspend fun createNewGlyph(chunkID: Int): GlyphCache.Entry {
        val image = BufferedImage(textureSize, textureSize, BufferedImage.TYPE_BYTE_GRAY)
        val charInfoArray = drawGlyphs(image, chunkID shl 9)
        val rawImage = dumpAlphaParallel(image)
        val data = ByteArray(BC4Compression.getEncodedSize(Mipmaps.getTotalSize(rawImage.data.size, 4)))

        coroutineScope {
            var offset = 0
            Mipmaps.generate(rawImage, 4).collect {
                val size = BC4Compression.getEncodedSize(it.data.size)
                val i = offset
                offset += size

                launch {
                    val view = ByteBuffer.wrap(data, i, size)
                    view.order(ByteOrder.nativeOrder())
                    BC4Compression.encode(it, view)
                }
            }
        }

        return GlyphCache.Entry(
            512,
            image.width,
            image.height,
            4,
            BC4Compression.getEncodedSize(rawImage.data.size),
            charInfoArray,
            data
        ).also {
            BackgroundScope.launch {
                GlyphCache.put(font, chunkID, it)
            }
        }
    }

    private fun drawGlyphs(bufferedImage: BufferedImage, chunkStart: Int): Array<CharInfo> {
        val graphics2D = bufferedImage.createGraphics()

        graphics2D.background = Color.BLACK
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

        coroutineScope {
            ParallelUtils.splitListIndex(size) { start, end ->
                launch {
                    val data = ByteArray(1)
                    for (i in start until end) {
                        image.raster.getDataElements(i % image.width, i / image.width, data)
                        outputData[i] = data[0]
                    }
                }
            }
        }

        return RawImage(outputData, image.width, image.height, 1)
    }

}