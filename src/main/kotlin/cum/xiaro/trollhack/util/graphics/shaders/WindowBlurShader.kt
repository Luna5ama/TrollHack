package cum.xiaro.trollhack.util.graphics.shaders

import cum.xiaro.trollhack.event.AlwaysListening
import cum.xiaro.trollhack.event.events.render.ResolutionUpdateEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import cum.xiaro.trollhack.util.graphics.MatrixUtils
import cum.xiaro.trollhack.util.graphics.use
import cum.xiaro.trollhack.util.interfaces.Helper
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.shader.Framebuffer
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.GL_QUADS
import org.lwjgl.opengl.GL20.*

object WindowBlurShader : AlwaysListening, Helper {
    private val framebuffer = Framebuffer(mc.displayWidth, mc.displayHeight, false)
    private val pass1 = Pass("/assets/trollhack/shaders/gui/WindowBlurH.vsh")
    private val pass2 = Pass("/assets/trollhack/shaders/gui/WindowBlurV.vsh")

    init {
        updateResolution(mc.displayWidth, mc.displayHeight)

        listener<ResolutionUpdateEvent>(true) {
            updateResolution(it.width, it.height)
        }
    }

    private fun updateResolution(width: Int, height: Int) {
        pass1.bind()
        pass1.updateResolution(width.toFloat(), height.toFloat())
        pass2.bind()
        pass2.updateResolution(width.toFloat(), height.toFloat())
        framebuffer.createBindFramebuffer(width, height)
    }

    fun render(x: Float, y: Float) {
        render(0.0f, 0.0f, x, y)
    }

    fun render(x1: Float, y1: Float, x2: Float, y2: Float) {
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer

        val x1D = x1.toDouble()
        val y1D = y1.toDouble()
        val x2D = x2.toDouble()
        val y2D = y2.toDouble()

        GlStateManager.enableTexture2D()
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit)
        GlStateManager.disableDepth()
        GlStateManager.depthMask(false)

        mc.framebuffer.bindFramebufferTexture()
        framebuffer.bindFramebuffer(false)
        GlStateUtils.blend(false)

        pass1.bind()
        pass1.updateMatrix()

        buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        buffer.pos(x1D, x1D, 1.0).endVertex()
        buffer.pos(x1D, y2D, -1.0).endVertex()
        buffer.pos(x2D, y2D, -1.0).endVertex()
        buffer.pos(x2D, y1D, 1.0).endVertex()
        tessellator.draw()

        framebuffer.bindFramebufferTexture()
        mc.framebuffer.bindFramebuffer(false)

        pass2.bind()
        pass2.updateMatrix()

        buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        buffer.pos(x1D, y1D, -1.0).endVertex()
        buffer.pos(x1D, y2D, -1.0).endVertex()
        buffer.pos(x2D, y2D, 1.0).endVertex()
        buffer.pos(x2D, y1D, 1.0).endVertex()
        tessellator.draw()

        framebuffer.unbindFramebufferTexture()
        mc.framebuffer.bindFramebuffer(false)
        GlStateUtils.blend(true)
    }

    private open class Pass(vertShaderPath: String) : DrawShader(vertShaderPath, "/assets/trollhack/shaders/gui/WindowBlur.fsh") {
        val reverseProjectionUniform = glGetUniformLocation(id, "reverseProjection")
        val resolutionUniform = glGetUniformLocation(id, "resolution")

        init {
            use {
                updateResolution(mc.displayWidth.toFloat(), mc.displayHeight.toFloat())
                glUniform1i(glGetUniformLocation(id, "background"), 0)
            }
        }

        fun updateResolution(width: Float, height: Float) {
            glUniform2f(resolutionUniform, width, height)

            val matrix = Matrix4f()
                .ortho(0.0f, width, 0.0f, height, 1000.0f, 3000.0f)
                .invert()

            MatrixUtils.loadMatrix(matrix).uploadMatrix(reverseProjectionUniform)
        }
    }
}