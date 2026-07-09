package dev.luna5ama.trollhack.graphics.skija

import dev.luna5ama.trollhack.event.impl.render.Skija2DEvent
import dev.luna5ama.trollhack.utils.Helper
import dev.luna5ama.trollhack.utils.compat.bindWrite
import dev.luna5ama.trollhack.utils.compat.frameBufferId
import io.github.humbleui.skija.BackendRenderTarget
import io.github.humbleui.skija.ColorSpace
import io.github.humbleui.skija.DirectContext
import io.github.humbleui.skija.FramebufferFormat
import io.github.humbleui.skija.Surface
import io.github.humbleui.skija.SurfaceColorFormat
import io.github.humbleui.skija.SurfaceOrigin
import org.lwjgl.opengl.GL11.GL_SCISSOR_TEST
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glViewport

object SkijaMinecraftBridge : Helper {
    var enabled = false
    private var directContext: DirectContext? = null

    fun render2D(ticksDelta: Float) {
        if (!enabled) return
        if (!Skija2DEvent.hasListeners) return
        if (!com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) return

        val framebufferWidth = mc.window.width
        val framebufferHeight = mc.window.height
        if (framebufferWidth <= 0 || framebufferHeight <= 0) return

        val context = directContext ?: DirectContext.makeGL().also {
            directContext = it
        }

        mc.mainRenderTarget.bindWrite(false)
        glViewport(0, 0, framebufferWidth, framebufferHeight)
        glDisable(GL_SCISSOR_TEST)

        val scale = mc.window.guiScale.toFloat()

        BackendRenderTarget.makeGL(
            framebufferWidth,
            framebufferHeight,
            0,
            8,
            mc.mainRenderTarget.frameBufferId,
            FramebufferFormat.GR_GL_RGBA8
        ).use { target ->
            Surface.wrapBackendRenderTarget(
                context,
                target,
                SurfaceOrigin.BOTTOM_LEFT,
                SurfaceColorFormat.RGBA_8888,
                ColorSpace.getSRGB()
            ).use { surface ->
                val canvas = surface.canvas
                canvas.save()
                canvas.scale(scale, scale)
                Skija2DEvent(
                    context = context,
                    surface = surface,
                    canvas = canvas,
                    framebufferWidth = framebufferWidth,
                    framebufferHeight = framebufferHeight,
                    scaledWidth = framebufferWidth / scale,
                    scaledHeight = framebufferHeight / scale,
                    scale = scale,
                    ticksDelta = ticksDelta
                ).post()
                canvas.restore()
                surface.flushAndSubmit()
            }
        }

        context.resetGLAll()
        mc.mainRenderTarget.bindWrite(false)
    }

    fun close() {
        directContext?.close()
        directContext = null
    }
}
