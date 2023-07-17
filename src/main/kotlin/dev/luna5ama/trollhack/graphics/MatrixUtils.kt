package dev.luna5ama.trollhack.graphics

import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.OpenGlHelper.glUniformMatrix4
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL41.glProgramUniformMatrix4
import java.nio.FloatBuffer

object MatrixUtils {
    val matrixBuffer: FloatBuffer = GLAllocation.createDirectFloatBuffer(16)

    fun loadProjectionMatrix(): MatrixUtils {
        matrixBuffer.clear()
        glGetFloat(GL_PROJECTION_MATRIX, matrixBuffer)
        return this
    }

    fun loadModelViewMatrix(): MatrixUtils {
        matrixBuffer.clear()
        glGetFloat(GL_MODELVIEW_MATRIX, matrixBuffer)
        return this
    }

    fun loadMatrix(matrix: Matrix4f): MatrixUtils {
        matrix.get(matrixBuffer)
        return this
    }

    fun getMatrix(): Matrix4f {
        return Matrix4f(matrixBuffer)
    }

    fun getMatrix(matrix: Matrix4f): Matrix4f {
        matrix.set(matrixBuffer)
        return matrix
    }

    fun uploadMatrix(location: Int) {
        glUniformMatrix4(location, false, matrixBuffer)
    }

    fun uploadMatrix(id: Int, location: Int) {
        glProgramUniformMatrix4(id, location, false, matrixBuffer)
    }
}