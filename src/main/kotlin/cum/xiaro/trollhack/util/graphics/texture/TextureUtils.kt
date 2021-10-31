package cum.xiaro.trollhack.util.graphics.texture

import net.minecraft.client.renderer.GLAllocation
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_BGRA
import org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8_REV
import org.lwjgl.opengl.GL30.GL_R8
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer

object TextureUtils {
    private val buffer = GLAllocation.createDirectByteBuffer(0x4000000)
    private val intBuffer = buffer.asIntBuffer()

    fun uploadRGBA(bufferedImage: BufferedImage, format: Int) {
        uploadRGBA(bufferedImage.getRGB(), format, bufferedImage.width, bufferedImage.height)
    }

    fun uploadRGBA(data: IntArray, format: Int, width: Int, height: Int) {
        intBuffer.put(data)

        buffer.flip()
        glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, buffer)
        buffer.clear()
    }

    fun uploadAlpha(bufferedImage: BufferedImage) {
        uploadAlpha(bufferedImage.getAlpha(), bufferedImage.width, bufferedImage.height)
    }

    fun uploadAlpha(data: ByteArray, width: Int, height: Int) {
        buffer.put(data)

        buffer.flip()
        glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, width, height, 0, GL_ALPHA, GL_UNSIGNED_BYTE, buffer)
        buffer.clear()
    }

    fun uploadRed(bufferedImage: BufferedImage) {
        uploadAlpha(bufferedImage.getAlpha(), bufferedImage.width, bufferedImage.height)
    }

    fun uploadRed(data: ByteArray, width: Int, height: Int) {
        buffer.put(data)

        buffer.flip()
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, width, height, 0, GL_RED, GL_UNSIGNED_BYTE, buffer)
        buffer.clear()
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
}