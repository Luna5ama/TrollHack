package dev.luna5ama.trollhack.graphics.texture

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL32.GL_TEXTURE_2D
import org.lwjgl.opengl.GL32.GL_TEXTURE_2D_MULTISAMPLE
import org.lwjgl.opengl.GL45.glCreateTextures

class FramebufferTexture(
    override var width: Int,
    override var height: Int,
    override var msaaLevel: Int
) : Texture {
    private var deleted = false
    override var id = glCreateTextures(if (msaaLevel == 0) GL_TEXTURE_2D else GL_TEXTURE_2D_MULTISAMPLE)
    override val available get() = !deleted
    override fun bindTexture() = GL11.glBindTexture(if (msaaLevel == 0) GL_TEXTURE_2D else GL_TEXTURE_2D_MULTISAMPLE, id)
    override fun unbindTexture() = GL11.glBindTexture(if (msaaLevel == 0) GL_TEXTURE_2D else GL_TEXTURE_2D_MULTISAMPLE, 0)
    override fun deleteTexture() {
        GL11.glDeleteTextures(id)
        deleted = true
    }
}