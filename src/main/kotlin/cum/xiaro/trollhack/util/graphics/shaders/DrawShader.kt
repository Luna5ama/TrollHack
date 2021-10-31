package cum.xiaro.trollhack.util.graphics.shaders

import cum.xiaro.trollhack.util.graphics.MatrixUtils
import org.joml.Matrix4f
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glUniformMatrix4
import java.nio.FloatBuffer

open class DrawShader(vertShaderPath: String, fragShaderPath: String) : Shader(vertShaderPath, fragShaderPath) {
    private val projectionUniform = glGetUniformLocation(id, "projection")
    private val modelViewUniform = glGetUniformLocation(id, "modelView")

    fun updateMatrix() {
        updateModelViewMatrix()
        updateProjectionMatrix()
    }

    fun updateProjectionMatrix() {
        MatrixUtils.loadProjectionMatrix().uploadMatrix(projectionUniform)
    }

    fun updateProjectionMatrix(matrix: Matrix4f) {
        MatrixUtils.loadMatrix(matrix).uploadMatrix(projectionUniform)
    }

    fun uploadProjectionMatrix(buffer: FloatBuffer) {
        glUniformMatrix4(projectionUniform, false, buffer)
    }

    fun updateModelViewMatrix() {
        MatrixUtils.loadModelViewMatrix().uploadMatrix(modelViewUniform)
    }

    fun updateModelViewMatrix(matrix: Matrix4f) {
        MatrixUtils.loadMatrix(matrix).uploadMatrix(modelViewUniform)
    }

    fun uploadModelViewMatrix(buffer: FloatBuffer) {
        glUniformMatrix4(modelViewUniform, false, buffer)
    }
}