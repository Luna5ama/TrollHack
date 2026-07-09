package dev.luna5ama.trollhack.graphics.texture

import dev.luna5ama.trollhack.utils.createDirectByteBuffer
import dev.luna5ama.trollhack.utils.timing.TickTimer
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL45.*
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.system.measureTimeMillis

/**
 * @author SpartanB312
 */
object ImageUtils {

    private const val DEFAULT_BUFFER_SIZE = 0x6400000
    private var byteBuffer = createDirectByteBuffer(DEFAULT_BUFFER_SIZE) // Max 64 MB
    private val reallocateTimer = TickTimer()

    /**
     * Dynamic memory allocation
     */
    internal fun putIntArray(intArray: IntArray): IntBuffer {
        if (intArray.size * 4 > byteBuffer.capacity()) {
            byteBuffer.clear()
            byteBuffer = createDirectByteBuffer(intArray.size * 4)
            reallocateTimer.reset()
        } else if (
            byteBuffer.capacity() > DEFAULT_BUFFER_SIZE
            && intArray.size * 4 < DEFAULT_BUFFER_SIZE
            && reallocateTimer.tick(1000)
        ) {
            byteBuffer.clear()
            byteBuffer = createDirectByteBuffer(DEFAULT_BUFFER_SIZE)
        }

        byteBuffer.asIntBuffer().apply {
            clear()
            put(intArray)
            flip()
            return this
        }
    }

    fun putByteArray(byteArray: ByteArray): ByteBuffer {
        if (byteArray.size > byteBuffer.capacity()) {
            byteBuffer.clear()
            byteBuffer = createDirectByteBuffer(byteArray.size)
            reallocateTimer.reset()
        } else if (
            byteBuffer.capacity() > DEFAULT_BUFFER_SIZE
            && byteArray.size < DEFAULT_BUFFER_SIZE
            && reallocateTimer.tick(1000)
        ) {
            byteBuffer.clear()
            byteBuffer = createDirectByteBuffer(DEFAULT_BUFFER_SIZE)
        }

        byteBuffer.apply {
            clear()
            put(byteArray)
            flip()
            return this
        }
    }

    fun uploadImage(texture: AbstractTexture, bufferedImage: BufferedImage, format: Int, width: Int, height: Int) {
        val array = IntArray(width * height)
        bufferedImage.getRGB(0, 0, width, height, array, 0, width)

        // Upload image
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0)
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0)
        // BGRA TO RGBA
        for (index in 0 until (width * height)) {
            array[index] = array[index] and -0x1000000 or
                    (array[index] and 0x00FF0000 shr 16) or
                    (array[index] and 0x0000FF00) or
                    (array[index] and 0x000000FF shl 16)
        }
        if (texture.state.get() == AbstractTexture.State.CREATED) {
            glTextureStorage2D(texture.id, texture.storageLevels, GL_RGBA8, width, height)
        }
        glTextureSubImage2D(
            texture.id,
            0,
            0,
            0,
            width,
            height,
            format,
            GL_UNSIGNED_BYTE,
            array
        )
    }

}
