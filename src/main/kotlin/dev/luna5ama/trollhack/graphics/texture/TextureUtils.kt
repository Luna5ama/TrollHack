package dev.luna5ama.trollhack.graphics.texture

import dev.luna5ama.kmogus.byteLength
import dev.luna5ama.kmogus.memcpy
import dev.luna5ama.trollhack.graphics.glMapNamedBufferRange
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.math.MathUtils
import net.minecraft.client.resources.IResource
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_BGRA
import org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8_REV
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL15.glDeleteBuffers
import org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER
import org.lwjgl.opengl.GL30.GL_MAP_WRITE_BIT
import org.lwjgl.opengl.GL30.GL_R8
import org.lwjgl.opengl.GL43.glInvalidateBufferData
import org.lwjgl.opengl.GL45.*
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import javax.imageio.ImageIO
import kotlin.math.max

object TextureUtils {
    private var bufferID = 0
    private var bufferSize = -1L

    fun uploadRGBA(bufferedImage: BufferedImage, format: Int) {
        uploadRGBA(bufferedImage.getRGB(), format, bufferedImage.width, bufferedImage.height)
    }

    fun uploadRGBA(data: IntArray, format: Int, width: Int, height: Int) {
        putData(data)
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, bufferID)
        glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, 0L)
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0)
    }

    fun uploadAlpha(bufferedImage: BufferedImage) {
        uploadAlpha(bufferedImage.getAlpha(), bufferedImage.width, bufferedImage.height)
    }

    fun uploadAlpha(data: ByteArray, width: Int, height: Int) {
        putData(data)
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, bufferID)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, width, height, 0, GL_ALPHA, GL_UNSIGNED_BYTE, 0L)
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0)
    }

    fun uploadRed(bufferedImage: BufferedImage) {
        uploadAlpha(bufferedImage.getAlpha(), bufferedImage.width, bufferedImage.height)
    }

    fun uploadRed(data: ByteArray, width: Int, height: Int) {
        putData(data)
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, bufferID)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, width, height, 0, GL_RED, GL_UNSIGNED_BYTE, 0L)
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0)
    }

    fun BufferedImage.getAlpha(): ByteArray {
        val numBands = raster.numBands

        val data = when (val dataType = raster.dataBuffer.dataType) {
            DataBuffer.TYPE_BYTE -> ByteArray(numBands)
            DataBuffer.TYPE_USHORT -> ShortArray(numBands)
            DataBuffer.TYPE_INT -> IntArray(numBands)
            DataBuffer.TYPE_FLOAT -> FloatArray(numBands)
            DataBuffer.TYPE_DOUBLE -> DoubleArray(numBands)
            else -> throw IllegalArgumentException("Unknown data buffer type: $dataType")
        }

        val alphaArray = ByteArray(width * height)
        var index = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                alphaArray[index++] = colorModel.getAlpha(raster.getDataElements(x, y, data)).toByte()
            }
        }

        return alphaArray
    }

    fun BufferedImage.getRGB(): IntArray {
        val numBands = raster.numBands

        val data = when (val dataType = raster.dataBuffer.dataType) {
            DataBuffer.TYPE_BYTE -> ByteArray(numBands)
            DataBuffer.TYPE_USHORT -> ShortArray(numBands)
            DataBuffer.TYPE_INT -> IntArray(numBands)
            DataBuffer.TYPE_FLOAT -> FloatArray(numBands)
            DataBuffer.TYPE_DOUBLE -> DoubleArray(numBands)
            else -> throw IllegalArgumentException("Unknown data buffer type: $dataType")
        }

        val rgbArray = IntArray(width * height)
        var index = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                rgbArray[index++] = colorModel.getRGB(raster.getDataElements(x, y, data))
            }
        }

        return rgbArray
    }

    private fun putData(data: ByteArray) {
        val length = data.byteLength
        checkBuffer(length)
        val arr = glMapNamedBufferRange(bufferID, 0, length, GL_MAP_WRITE_BIT)
        memcpy(data, arr.ptr, length)
        glUnmapNamedBuffer(bufferID)
    }

    private fun putData(data: IntArray) {
        val length = data.byteLength
        checkBuffer(length)
        val arr = glMapNamedBufferRange(bufferID, 0, length, GL_MAP_WRITE_BIT)
        memcpy(data, arr.ptr, length)
        glUnmapNamedBuffer(bufferID)
    }

    private fun checkBuffer(capacity: Long) {
        if (capacity > bufferSize) {
            if (bufferID != 0) {
                glDeleteBuffers(bufferID)
            }
            bufferID = glCreateBuffers()
            glNamedBufferStorage(bufferID, max(capacity, bufferSize * 2), GL_MAP_WRITE_BIT)
            bufferSize = capacity
            return
        }

        glInvalidateBufferData(bufferID)
    }

    @JvmStatic
    fun combineTexturesVertically(images: Array<BufferedImage>): BufferedImage {
        check(images.isNotEmpty())

        val firstImage = images[0]
        val height = firstImage.height
        val totalHeight = MathUtils.ceilToPOT(height * images.size)
        val finalImage = BufferedImage(firstImage.width, totalHeight, firstImage.type)
        val graphics = finalImage.createGraphics()

        for (i in images.indices) {
            val src = images[i]
            graphics.drawImage(src, 0, height * i, null)
        }

        graphics.dispose()

        return finalImage
    }

    @JvmStatic
    fun ResourceLocation.readImage(): BufferedImage {
        return Wrapper.minecraft.resourceManager.getResource(this).readImage()
    }

    @JvmStatic
    fun IResource.readImage(): BufferedImage {
        return this.inputStream.use {
            ImageIO.read(it)
        }
    }
}