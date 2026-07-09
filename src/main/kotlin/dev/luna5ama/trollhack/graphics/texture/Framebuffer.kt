package dev.luna5ama.trollhack.graphics.texture

import dev.luna5ama.trollhack.graphics.GLHelper
import dev.luna5ama.trollhack.graphics.GLObject
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30

interface Framebuffer : GLObject {
    val width: Int
    val height: Int
    val useRBO: Boolean
    val clearColor: ColorRGBA
    val fbo: Int
    val rbo: Int
    val colorAttachments: MutableList<ColorLayer>
    var msaaLevel: Int

    val texture get() = colorAttachments[0].texture
    override val id get() = fbo

    fun bindFramebuffer(viewPort: Boolean = false) {
        GLHelper.bindFramebuffer(fbo, true)
        if (viewPort) GL11.glViewport(0, 0, width, height)
    }

    fun unbindFramebuffer() = GLHelper.bindFramebuffer(0)

    fun delete(colorAttachments: Boolean = true) {
        if (useRBO) GL30.glDeleteRenderbuffers(rbo)
        GL30.glDeleteFramebuffers(fbo)
        GLHelper
        if (colorAttachments) this.colorAttachments.forEach { it.texture.deleteTexture() }
    }

    override fun bind() {
        bindFramebuffer()
    }

    override fun unbind() {
        unbindFramebuffer()
    }

    override fun destroy() {
        delete()
    }

    interface ColorLayer : Texture {
        val index: Int
        val framebuffer: Framebuffer
        val texture: FramebufferTexture
        val attachmentIndex get() = GL30.GL_COLOR_ATTACHMENT0 + index

        fun bindLayer() = GL11.glDrawBuffer(attachmentIndex)
        fun delete() = texture.deleteTexture()
    }
}