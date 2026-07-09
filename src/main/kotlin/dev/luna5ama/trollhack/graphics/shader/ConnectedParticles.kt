package dev.luna5ama.trollhack.graphics.shader

import dev.luna5ama.trollhack.RenderSystem
import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.impl.render.ResolutionUpdateEvent
import dev.luna5ama.trollhack.utils.Helper
import dev.luna5ama.trollhack.graphics.GLHelper
import dev.luna5ama.trollhack.graphics.buffer.VertexFormat
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects.draw
import dev.luna5ama.trollhack.graphics.shader.ConnectedParticles.DrawBuffer
import dev.luna5ama.trollhack.graphics.shader.ConnectedParticles.noiseTexture
import dev.luna5ama.trollhack.graphics.texture.MipmapTexture
import dev.luna5ama.trollhack.graphics.texture.ResizableFramebuffer
import dev.luna5ama.trollhack.graphics.use
import org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP
import org.lwjgl.opengl.GL30.*

object ConnectedParticles : Helper, Shader("/assets/shader/background/nopVertex.vsh", "/assets/shader/background/connectedParticles.fsh") {
    private val iTimeUniform = getUniformLocation("iTime")
    private val iResolutionUniform = getUniformLocation("iResolution")
    private val backgroundAlphaUniform = getUniformLocation("backgroundAlpha")
    private val iFrameUniform = getUniformLocation("iFrame")
    private val iMouseUniform = getUniformLocation("iMouse")
    private val createTime = System.currentTimeMillis()
    val noiseTexture = MipmapTexture("assets/shader/background/noise.png")
    private var iFrame = 1

    init {
        useShader {
            glUniform1i(getUniformLocation("background"), 0)
            glUniform1i(getUniformLocation("iChannel0"), 1)
        }
    }

    fun draw(alpha: Float) {
        Channel0Pass.draw()
        use {
            GLHelper.depth = false
            glUniform1f(iTimeUniform, (System.currentTimeMillis() - createTime) / 1000f)
            glUniform2f(iResolutionUniform, RenderSystem.width.toFloat(), RenderSystem.height.toFloat())
            glUniform1f(backgroundAlphaUniform, 1f)
            glUniform1i(iFrameUniform, iFrame)
            glUniform2f(iMouseUniform, mc.mouseHandler.xpos().toFloat() / RenderSystem.widthF, (RenderSystem.heightF - mc.mouseHandler.ypos().toFloat()) / RenderSystem.heightF)

            glActiveTexture(GL_TEXTURE1)
            Channel0Pass.channel0FrameBuffer.texture.bindTexture()

            GL_TRIANGLE_STRIP.draw(DrawBuffer, ConnectedParticles) {
                vertex(1f, -1f)
                vertex(-1f, -1f)
                vertex(1f, 1f)
                vertex(-1f, 1f)
            }

            glActiveTexture(GL_TEXTURE1)
            Channel0Pass.channel0FrameBuffer.texture.unbindTexture()

            glActiveTexture(GL_TEXTURE0)
        }
        iFrame++
    }

    object DrawBuffer : PMVBObjects.VertexMode(VertexFormat.Pos2f.attribute, ConnectedParticles, 4) {
        fun vertex(x: Float, y: Float) {
            val pointer = arr.ptr

            pointer[0] = x
            pointer[4] = y

            arr += 8
            vertexSize += 1
        }
    }


}

object Channel0Pass : Helper, Shader(
    "/assets/shader/background/nopVertex.vsh",
    "/assets/shader/background/connectedParticlesChannel0.fsh"), AlwaysListening {
    private val iTimeUniform = getUniformLocation("iTime")
    private val iResolutionUniform = getUniformLocation("iResolution")
    private val iFrameUniform = getUniformLocation("iFrame")
    private val createTime = System.currentTimeMillis()
    val channel0FrameBuffer = ResizableFramebuffer(RenderSystem.framebuffer.width, RenderSystem.framebuffer.height, true, 0)
    private var iFrame = 1

    init {
        channel0FrameBuffer.generateColorLayer()

        useShader {
            glUniform1i(getUniformLocation("iChannel1"), 0)
            glUniform1i(getUniformLocation("iChannel0"), 1)
        }

        handler<ResolutionUpdateEvent> {
            channel0FrameBuffer.resize(it.framebufferWidth, it.framebufferHeight)
        }
    }

    fun draw() {
        channel0FrameBuffer.bind()
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, channel0FrameBuffer.fbo)
        glBindFramebuffer(GL_READ_FRAMEBUFFER, channel0FrameBuffer.fbo)
        use {
            GLHelper.depth = false
            glUniform1f(iTimeUniform, (System.currentTimeMillis() - createTime) / 1000f)
            glUniform2f(iResolutionUniform, RenderSystem.width.toFloat(), RenderSystem.height.toFloat())
            glUniform1i(iFrameUniform, iFrame)

            glActiveTexture(GL_TEXTURE0)
            noiseTexture.bindTexture()

            glActiveTexture(GL_TEXTURE1)
            channel0FrameBuffer.texture.bindTexture()

            GL_TRIANGLE_STRIP.draw(DrawBuffer, Channel0Pass) {
                vertex(1f, -1f)
                vertex(-1f, -1f)
                vertex(1f, 1f)
                vertex(-1f, 1f)
            }

            glActiveTexture(GL_TEXTURE0)
            noiseTexture.unbindTexture()

            glActiveTexture(GL_TEXTURE1)
            channel0FrameBuffer.texture.unbindTexture()

            glActiveTexture(GL_TEXTURE0)
        }
        iFrame++
        RenderSystem.framebuffer.bind()
    }
}
