package dev.luna5ama.trollhack.graphics.shaders

import dev.luna5ama.trollhack.event.AlwaysListening
import dev.luna5ama.trollhack.event.events.render.ResolutionUpdateEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.graphics.GLDataType
import dev.luna5ama.trollhack.graphics.GlStateUtils
import dev.luna5ama.trollhack.graphics.MatrixUtils
import dev.luna5ama.trollhack.graphics.buffer.PersistentMappedVBO
import dev.luna5ama.trollhack.graphics.buildAttribute
import dev.luna5ama.trollhack.graphics.use
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.structs.Vec4f32
import dev.luna5ama.trollhack.util.interfaces.Helper
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.shader.Framebuffer
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glUniform1i
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL41.glProgramUniform2f
import org.lwjgl.opengl.GL45.glTextureParameteri

object WindowBlurShader : AlwaysListening, Helper {
    private val vao = PersistentMappedVBO.createVao(buildAttribute(16) {
        float(0, 4, GLDataType.GL_FLOAT, false)
    })

    private val fbo1 = Framebuffer(mc.displayWidth, mc.displayHeight, false)
    private val fbo2 = Framebuffer(mc.displayWidth, mc.displayHeight, false)
    private val passH = Pass("/assets/trollhack/shaders/gui/WindowBlurH.vsh")
    private val passV = Pass("/assets/trollhack/shaders/gui/WindowBlurV.vsh")

    init {
        updateResolution(mc.displayWidth, mc.displayHeight)

        listener<ResolutionUpdateEvent>(true) {
            updateResolution(it.width, it.height)
        }
    }

    private fun updateResolution(width: Int, height: Int) {
        passH.updateResolution(width.toFloat(), height.toFloat())
        passV.updateResolution(width.toFloat(), height.toFloat())
        fbo1.createBindFramebuffer(width, height)
        setTextureParam(fbo1.framebufferTexture)
        fbo2.createBindFramebuffer(width, height)
        setTextureParam(fbo2.framebufferTexture)
    }

    fun render(x: Float, y: Float) {
        render(0.0f, 0.0f, x, y)
    }

    fun render(x1: Float, y1: Float, x2: Float, y2: Float) {
        val pass = GuiSetting.windowBlurPass
        if (pass == 0) return

        setTextureParam(mc.framebuffer.framebufferTexture)
        putVertices(x1, y1, x2, y2)

        GlStateManager.enableTexture2D()
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit)
        GlStateManager.disableDepth()
        GlStateManager.depthMask(false)
        GlStateUtils.blend(false)

        passH.updateMatrix()
        passV.updateMatrix()
        glBindVertexArray(vao)

        var extend = pass - 1.0f

        bindFbo(mc.framebuffer, fbo1)
        drawPass(passH, extend, extend + 1.0f)

        while (extend > 0) {
            bindFbo(fbo1, fbo2)
            drawPass(passV, extend, extend)

            bindFbo(fbo2, fbo1)
            drawPass(passH, extend - 1.0f, extend)

            extend--
        }

        bindFbo(fbo1, mc.framebuffer)
        drawPass(passV, 0.0f, 0.0f)

        fbo1.unbindFramebufferTexture()
        GlStateUtils.blend(true)
        PersistentMappedVBO.end()
        glBindVertexArray(0)
    }

    private fun drawPass(shader: Pass, x: Float, y: Float) {
        shader.bind()
        shader.updateExtend(x, y)
        glDrawArrays(GL_TRIANGLES, PersistentMappedVBO.drawOffset, 6)
    }

    private fun bindFbo(from: Framebuffer, to: Framebuffer) {
        from.bindFramebufferTexture()
        to.bindFramebuffer(false)
    }

    private fun putVertices(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
    ) {
        val array = PersistentMappedVBO.arr
        var struct = Vec4f32(array)

        struct.x = x1
        struct.y = y1
        struct.z = -1.0f
        struct.w = 1.0f
        struct++

        struct.x = x1
        struct.y = y2
        struct.z = -1.0f
        struct.w = -1.0f
        struct++

        struct.x = x2
        struct.y = y2
        struct.z = 1.0f
        struct.w = -1.0f
        struct++

        struct.x = x2
        struct.y = y1
        struct.z = 1.0f
        struct.w = 1.0f
        struct++

        struct.x = x1
        struct.y = y1
        struct.z = -1.0f
        struct.w = 1.0f
        struct++

        struct.x = x2
        struct.y = y2
        struct.z = 1.0f
        struct.w = -1.0f
        struct++

        array.pos(struct.ptr)
    }

    private fun setTextureParam(textureID: Int) {
        glTextureParameteri(textureID, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTextureParameteri(textureID, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTextureParameteri(textureID, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTextureParameteri(textureID, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    }

    private open class Pass(vertShaderPath: String) :
        DrawShader(vertShaderPath, "/assets/trollhack/shaders/gui/WindowBlur.fsh") {
        val reverseProjectionUniform = glGetUniformLocation(id, "reverseProjection")
        val resolutionUniform = glGetUniformLocation(id, "resolution")
        val extendUniform = glGetUniformLocation(id, "extend")

        init {
            use {
                updateResolution(mc.displayWidth.toFloat(), mc.displayHeight.toFloat())
                glUniform1i(glGetUniformLocation(id, "background"), 0)
            }
        }

        fun updateExtend(x: Float, y: Float) {
            glProgramUniform2f(id, extendUniform, x, y)
        }

        fun updateResolution(width: Float, height: Float) {
            glProgramUniform2f(id, resolutionUniform, width, height)

            val matrix = Matrix4f()
                .ortho(0.0f, width, 0.0f, height, 1000.0f, 3000.0f)
                .invert()

            MatrixUtils.loadMatrix(matrix).uploadMatrix(id, reverseProjectionUniform)
        }
    }
}