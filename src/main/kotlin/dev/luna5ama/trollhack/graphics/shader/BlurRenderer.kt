package dev.luna5ama.trollhack.graphics.shader

import com.mojang.blaze3d.pipeline.RenderTarget
import dev.luna5ama.trollhack.RenderSystem
import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.impl.render.ResolutionUpdateEvent
import dev.luna5ama.trollhack.event.impl.world.WorldEvent
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.Helper
import dev.luna5ama.trollhack.utils.compat.bindWrite
import dev.luna5ama.trollhack.utils.compat.colorTextureId
import dev.luna5ama.trollhack.graphics.GLDataType
import dev.luna5ama.trollhack.graphics.GLHelper
import dev.luna5ama.trollhack.graphics.buffer.buildAttribute
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PersistentMappedVBO
import dev.luna5ama.trollhack.graphics.matrix.getFloatArray
import dev.luna5ama.trollhack.graphics.texture.Framebuffer
import dev.luna5ama.trollhack.graphics.texture.ResizableFramebuffer
import dev.luna5ama.trollhack.graphics.use
import org.joml.Matrix4f
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL45.*

object BlurRenderer : Helper, AlwaysListening {

    private val attribute = buildAttribute(16) { float(0, 4, GLDataType.GL_FLOAT, false) }
    private val vao = PersistentMappedVBO.createVao(attribute)

    private val passH = Pass("/assets/shader/general/BlurH.vsh")
    private val passHMS = Pass("/assets/shader/general/BlurH.vsh", true)
    private val passV = Pass("/assets/shader/general/BlurV.vsh")
    private val passVMS = Pass("/assets/shader/general/BlurV.vsh", true)
    private var fbo1 = ResizableFramebuffer(mc.window.width, mc.window.height, true, 0)
    private var fbo2 = ResizableFramebuffer(mc.window.width, mc.window.height, true, 0)

    init {
        fbo1.generateColorLayer()
        fbo2.generateColorLayer()

        handler<ResolutionUpdateEvent> {
            updateResolution(it.framebufferWidth, it.framebufferHeight)
        }

        handler<WorldEvent.Load> {
            updateResolution(mc.window.width, mc.window.height)
        }
    }

    fun updateResolution(width: Int, height: Int) {
        fbo1.resize(width, height)
        fbo2.resize(width, height)
        listOf(passH, passHMS, passV, passVMS).forEach { pass ->
            pass.use {
                updateResolution(width.toFloat(), height.toFloat())
            }
        }
        setTextureParam(fbo1.texture.id)
        setTextureParam(fbo2.texture.id)
    }

    fun render(startX: Float, startY: Float, endX: Float, endY: Float, pass: Int = ClientSettings.windowBlurPass) {
        val useFrameBuffer = RenderSystem.useFramebuffer
        val msaaLevel = RenderSystem.msaaLevel
        if (pass == 0) return

        putVertex(startX, startY, endX, endY)
        GLHelper.blend = false
        GLHelper.depth = false
        passH.updateMatrix(RenderSystem.matrixLayer.matrixArray)
        passHMS.updateMatrix(RenderSystem.matrixLayer.matrixArray)
        passV.updateMatrix(RenderSystem.matrixLayer.matrixArray)
        passVMS.updateMatrix(RenderSystem.matrixLayer.matrixArray)

        GLHelper.bindVertexArray(vao)

        var extendPasses = pass - 1f

        if (useFrameBuffer) bindFbo(RenderSystem.framebuffer, fbo1)
        else bindFbo(mc.mainRenderTarget, fbo1)

        if (useFrameBuffer && msaaLevel != 0) drawPass(passHMS, extendPasses, extendPasses + 1.0f)
        else drawPass(passH, extendPasses, extendPasses + 1.0f)

        while (extendPasses > 0) {
            bindFbo(fbo1, fbo2)
            drawPass(passV, extendPasses, extendPasses)

            bindFbo(fbo2, fbo1)
            drawPass(passH, extendPasses - 1.0f, extendPasses)
            extendPasses--
        }

        if (useFrameBuffer) bindFbo(fbo1, RenderSystem.framebuffer)
        else bindFbo(fbo1, mc.mainRenderTarget)

        drawPass(passV, 0.0f, 0.0f)

        fbo1.texture.unbindTexture()
        GLHelper.blend = true

        PersistentMappedVBO.end(16)

        if (RenderSystem.useFramebuffer) {
            RenderSystem.framebuffer.bindFramebuffer()
            RenderSystem.defaultRenderLayer.bindLayer()
        } else mc.mainRenderTarget.bindWrite(false)
    }

    private fun setTextureParam(textureID: Int) {
        glTextureParameteri(textureID, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTextureParameteri(textureID, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTextureParameteri(textureID, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTextureParameteri(textureID, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    }

    private fun putVertex(x1: Float, y1: Float, x2: Float, y2: Float) {
        val arr = PersistentMappedVBO.arr
        val pointer = arr.ptr

        pointer[0] = x1
        pointer[4] = y1
        pointer[8] = -1.0f
        pointer[12] = 1.0f

        pointer[16] = x1
        pointer[20] = y2
        pointer[24] = -1.0f
        pointer[28] = -1.0f

        pointer[32] = x2
        pointer[36] = y2
        pointer[40] = 1.0f
        pointer[44] = -1.0f

        pointer[48] = x2
        pointer[52] = y1
        pointer[56] = 1.0f
        pointer[60] = 1.0f

        pointer[64] = x1
        pointer[68] = y1
        pointer[72] = -1.0f
        pointer[76] = 1.0f

        pointer[80] = x2
        pointer[84] = y2
        pointer[88] = 1.0f
        pointer[92] = -1.0f

        arr += 96
    }

    private fun drawPass(shader: Pass, x: Float, y: Float) {
        shader.bind()
        shader.updateExtend(x, y)
        glDrawArrays(GL_TRIANGLES, PersistentMappedVBO.drawOffset, 6)
    }

    private fun bindFbo(from: Framebuffer, to: Framebuffer) {
        from.texture.bindTexture()
        to.bindFramebuffer(false)
    }

    private fun bindFbo(from: Framebuffer, to: RenderTarget) {
        from.texture.bindTexture()
        to.bindWrite(false)
    }

    private fun bindFbo(from: RenderTarget, to: Framebuffer) {
        glBindTexture(GL_TEXTURE_2D, from.colorTextureId)
        to.bindFramebuffer(false)
    }

    private class Pass(vsh: String, multisampling: Boolean = false) : Shader(vsh,
        if (multisampling) "/assets/shader/general/BlurMS.fsh" else "/assets/shader/general/Blur.fsh") {
        val matrixUniform = getUniformLocation("matrix")
        val reverseProjection = getUniformLocation("reverseProjection")
        val resolution = getUniformLocation("resolution")
        val extend = getUniformLocation("extend")
        val msaaSample = getUniformLocation("msaaSample")

        init {
            useShader {
                updateResolution(RenderSystem.widthF, RenderSystem.heightF)
                GL20.glUniform1i(getUniformLocation("background"), 0)
            }
        }

        fun updateMatrix(matrix: FloatArray) {
            use {
                GL20.glUniformMatrix4fv(matrixUniform, false, matrix)
                glUniform1i(msaaSample, RenderSystem.msaaLevel)
            }
        }

        fun updateResolution(width: Float, height: Float) {
            GL20.glUniform2f(resolution, width, height)
            val matrix = Matrix4f()
                .ortho(0.0f, width, 0.0f, height, 1000.0f, 3000.0f)
                .invert()
            GL20.glUniformMatrix4fv(reverseProjection, false, matrix.getFloatArray())
        }

        fun updateExtend(x: Float, y: Float) {
            GL20.glUniform2f(extend, x, y)
        }
    }
}
