package dev.luna5ama.trollhack.utils.compat

import com.mojang.blaze3d.opengl.GlDevice
import com.mojang.blaze3d.opengl.GlTexture
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import org.lwjgl.opengl.GL11.glViewport
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30.glBindFramebuffer

val RenderTarget.frameBufferId: Int
    get() {
        val device = RenderSystem.getDevice() as? GlDevice ?: return 0
        val color = getColorTexture() as? GlTexture ?: return 0
        return color.getFbo(device.directStateAccess(), getDepthTexture())
    }

val RenderTarget.colorTextureId: Int
    get() = (getColorTexture() as? GlTexture)?.glId() ?: 0

fun RenderTarget.bindWrite(setViewport: Boolean) {
    glBindFramebuffer(GL_FRAMEBUFFER, frameBufferId)
    if (setViewport) glViewport(0, 0, width, height)
}
