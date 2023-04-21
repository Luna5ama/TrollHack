package dev.luna5ama.trollhack.util.graphics.font

import dev.luna5ama.trollhack.util.graphics.texture.AbstractTexture
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.*
import org.lwjgl.opengl.GL14.GL_GENERATE_MIPMAP_HINT
import org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer

class GlyphTexture(data: ByteBuffer, override val width: Int, override val height: Int, levels: Int) :
    AbstractTexture() {
    init {
        // Generate texture id and bind it
        textureID = glGenTextures()
        bindTexture()

        // Setup mipmap levels
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, levels)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, levels)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0.0f)

        val internalFormat = if (isIntel) GL_R8 else GL_COMPRESSED_RED_RGTC1
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, GL_RED, GL_UNSIGNED_BYTE, data)

        glHint(GL_GENERATE_MIPMAP_HINT, GL_NICEST)
        glGenerateMipmap(GL_TEXTURE_2D)

        // Unbind texture
        unbindTexture()
    }

    private companion object {
        val isIntel by lazy {
            glGetString(GL_VENDOR).contains("Intel")
        }
    }
}