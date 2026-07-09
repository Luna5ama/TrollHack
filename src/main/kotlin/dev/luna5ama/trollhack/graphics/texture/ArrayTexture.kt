@file:Suppress("SetterBackingFieldAssignment")

package dev.luna5ama.trollhack.graphics.texture

import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import org.lwjgl.opengl.ARBSparseTexture.GL_TEXTURE_SPARSE_ARB
import org.lwjgl.opengl.ARBSparseTexture.GL_VIRTUAL_PAGE_SIZE_INDEX_ARB
import org.lwjgl.opengl.ARBSparseTexture.glTexturePageCommitmentEXT
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY
import org.lwjgl.opengl.GL45.*
import java.awt.image.BufferedImage
import java.nio.ByteBuffer

class ArrayTexture(
    images: List<BufferedImage>,
    layerCount: Int = images.size,
    override var width: Int = images[0].width,
    override var height: Int = images[0].height,
    val format: Int = GL_RGBA,
    val useSparseTexture: Boolean = false
) : Texture {
    override val id = glCreateTextures(GL_TEXTURE_2D_ARRAY)

    override var msaaLevel = 0
        set(value) {}

    private var deleted = false

    override val available: Boolean
        get() = !deleted


    init {
        val useRGBA = true
        if (useSparseTexture) {
            glTextureParameteri(id, GL_VIRTUAL_PAGE_SIZE_INDEX_ARB, 0)
            glTextureParameteri(id, GL_TEXTURE_SPARSE_ARB, GL_TRUE)
        }
        if (useRGBA) {
            glTextureStorage3D(id, 1, GL_RGBA8, width, height, layerCount)
        } else {
            glTextureStorage3D(id, 1, GL_BGRA, width, height, layerCount)
        }

        if (images.isNotEmpty()) {
            val buffer = ByteBuffer.allocateDirect(layerCount * width * height * 4).asIntBuffer()
            images.forEach {
                val array = IntArray(width * height)
                it.getRGB(0, 0, width, height, array, 0, width)
                if (useRGBA) {
                    for (index in 0 until (width * height)) {
                        array[index] = array[index] and -0x1000000 or
                                (array[index] and 0x00FF0000 shr 16) or
                                (array[index] and 0x0000FF00) or
                                (array[index] and 0x000000FF shl 16)
                    }
                }
                buffer.put(array)
            }
            buffer.flip()
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
            glPixelStorei(GL_UNPACK_SKIP_ROWS, 0)
            glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0)
            glTextureSubImage3D(
                id,
                0, 0, 0, 0,
                width, height, layerCount,
                format, GL_UNSIGNED_BYTE, buffer
            )
        }
        glTextureParameteri(id, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTextureParameteri(id, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTextureParameteri(id, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTextureParameteri(id, GL_TEXTURE_WRAP_T, GL_REPEAT)
    }

    override fun bindTexture() {
        glBindTexture(GL_TEXTURE_2D_ARRAY, id)
    }

    override fun deleteTexture() {
        deleted = true
        GL11.glDeleteTextures(id)
    }

    override fun unbindTexture() {
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0)
    }
}