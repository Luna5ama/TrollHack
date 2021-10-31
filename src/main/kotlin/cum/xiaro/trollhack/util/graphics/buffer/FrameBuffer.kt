package cum.xiaro.trollhack.util.graphics.buffer

import cum.xiaro.trollhack.util.graphics.GLObject
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper.glDeleteBuffers
import net.minecraft.client.renderer.texture.TextureUtil.glGenTextures
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer

open class FrameBuffer : GLObject {
    override val id = glGenFramebuffers()
    private val textureID = glGenTextures()

    open fun allocateFrameBuffer(width: Int, height: Int) {
        GlStateManager.bindTexture(textureID)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null as ByteBuffer?)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        GlStateManager.bindTexture(0)

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureID, 0)
    }

    fun bindTexture() {
        GlStateManager.bindTexture(textureID)
    }

    fun unbindTexture() {
        GlStateManager.bindTexture(0)
    }

    override fun bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, id)
    }

    override fun unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    override fun destroy() {
        glDeleteFramebuffers(id)
        glDeleteBuffers(textureID)
    }
}