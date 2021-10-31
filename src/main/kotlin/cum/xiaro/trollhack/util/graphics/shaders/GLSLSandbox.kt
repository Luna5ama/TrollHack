package cum.xiaro.trollhack.util.graphics.shaders

import cum.xiaro.trollhack.util.graphics.use
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11.GL_QUADS
import org.lwjgl.opengl.GL20.*

open class GLSLSandbox(fragShaderPath: String) : Shader("/assets/trollhack/shaders/menu/DefaultVertex.vsh", fragShaderPath) {
    private val timeUniform = glGetUniformLocation(id, "time")
    private val mouseUniform = glGetUniformLocation(id, "mouse")
    private val resolutionUniform = glGetUniformLocation(id, "resolution")

    fun render(width: Float, height: Float, mouseX: Float, mouseY: Float, initTime: Long) {
        use {
            glUniform2f(resolutionUniform, width, height)
            glUniform2f(mouseUniform, mouseX / width, (height - 1.0f - mouseY) / height)
            glUniform1f(timeUniform, ((System.currentTimeMillis() - initTime) / 1000.0).toFloat())

            val tessellator = Tessellator.getInstance()
            val buffer = tessellator.buffer

            buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
            buffer.pos(-1.0, -1.0, 0.2).endVertex()
            buffer.pos(1.0, -1.0, 0.2).endVertex()
            buffer.pos(1.0, 1.0, 0.2).endVertex()
            buffer.pos(-1.0, 1.0, 0.2).endVertex()
            tessellator.draw()
        }
    }
}