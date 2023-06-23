package dev.luna5ama.trollhack.graphics.font

import dev.luna5ama.trollhack.graphics.texture.AbstractTexture
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.*
import org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS
import org.lwjgl.opengl.GL30.GL_COMPRESSED_RED_RGTC1
import org.lwjgl.opengl.GL45.*

class GlyphTexture(override val width: Int, override val height: Int, levels: Int) : AbstractTexture() {
    init {
        textureID = glCreateTextures(GL_TEXTURE_2D)

        glTextureParameteri(textureID, GL_TEXTURE_MIN_LOD, 0)
        glTextureParameteri(textureID, GL_TEXTURE_MAX_LOD, levels)

        glTextureParameteri(textureID, GL_TEXTURE_BASE_LEVEL, 0)
        glTextureParameteri(textureID, GL_TEXTURE_MAX_LEVEL, levels)

        glTextureParameteri(textureID, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTextureParameteri(textureID, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        glTextureParameteri(textureID, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTextureParameteri(textureID, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        glTextureParameterf(textureID, GL_TEXTURE_LOD_BIAS, 0.0f)

        glTextureStorage2D(textureID, levels + 1, GL_COMPRESSED_RED_RGTC1, width, height)
    }
}