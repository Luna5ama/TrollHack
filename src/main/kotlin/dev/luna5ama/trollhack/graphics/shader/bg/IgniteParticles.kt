package dev.luna5ama.trollhack.graphics.shader.bg

import dev.luna5ama.trollhack.RenderSystem
import dev.luna5ama.trollhack.graphics.GLHelper
import dev.luna5ama.trollhack.graphics.buffer.VertexFormat
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects.draw
import dev.luna5ama.trollhack.graphics.shader.Shader
import dev.luna5ama.trollhack.graphics.use
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20

object IgniteParticles : Shader("/assets/shader/background/nopVertex.vsh", "/assets/shader/background/igniteParticles.fsh") {
    private val iTimeUniform = getUniformLocation("iTime")
    private val iResolutionUniform = getUniformLocation("iResolution")
    private val backgroundAlphaUniform = getUniformLocation("backgroundAlpha")
    private val createTime = System.currentTimeMillis()

    fun draw(alpha: Float) {
        use {
            GLHelper.depth = false
            GL20.glUniform1f(iTimeUniform, (System.currentTimeMillis() - createTime) / 1000f)
            GL20.glUniform2f(iResolutionUniform, RenderSystem.width.toFloat(), RenderSystem.height.toFloat())
            GL20.glUniform1f(backgroundAlphaUniform, alpha)
            GL11.GL_TRIANGLE_STRIP.draw(DrawBuffer) {
                vertex(1f, -1f)
                vertex(-1f, -1f)
                vertex(1f, 1f)
                vertex(-1f, 1f)
            }
        }
    }

    private object DrawBuffer : PMVBObjects.VertexMode(VertexFormat.Pos2f.attribute, IgniteParticles) {
        fun vertex(x: Float, y: Float) {
            val pointer = arr.ptr

            pointer[0] = x
            pointer[4] = y

            arr += 8
            vertexSize += 1
        }
    }
}