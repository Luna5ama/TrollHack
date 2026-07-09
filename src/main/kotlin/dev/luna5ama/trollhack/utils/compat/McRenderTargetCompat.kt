package dev.luna5ama.trollhack.utils.compat

import com.mojang.blaze3d.opengl.GlTexture
import com.mojang.blaze3d.pipeline.RenderTarget
import org.lwjgl.opengl.GL11.glViewport
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING
import org.lwjgl.opengl.GL30.glBindFramebuffer
import org.lwjgl.opengl.GL30.glGetInteger

val RenderTarget.frameBufferId: Int
    get() = glGetInteger(GL_FRAMEBUFFER_BINDING)

val RenderTarget.colorTextureId: Int
    get() = (getColorTexture() as? GlTexture)?.glId() ?: 0

fun RenderTarget.bindWrite(setViewport: Boolean) {
    glBindFramebuffer(GL_FRAMEBUFFER, frameBufferId)
    if (setViewport) glViewport(0, 0, width, height)
}
