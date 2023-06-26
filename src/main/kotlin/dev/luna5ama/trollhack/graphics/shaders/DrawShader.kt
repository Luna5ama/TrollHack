package dev.luna5ama.trollhack.graphics.shaders

import dev.luna5ama.trollhack.graphics.MatrixUtils
import org.joml.Matrix4f
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glUniformMatrix4
import org.lwjgl.opengl.GL41.glProgramUniformMatrix4
import java.nio.FloatBuffer

open class DrawShader(vertShaderPath: String, fragShaderPath: String) : Shader(vertShaderPath, fragShaderPath) {
    private val projectionUniform = glGetUniformLocation(id, "projection")
    private val modelViewUniform = glGetUniformLocation(id, "modelView")

    fun updateMatrix() {
        updateModelViewMatrix()
        updateProjectionMatrix()
    }

    fun updateProjectionMatrix() {
        MatrixUtils.loadProjectionMatrix().uploadMatrix(id, projectionUniform)
    }

    fun updateProjectionMatrix(matrix: Matrix4f) {
        MatrixUtils.loadMatrix(matrix).uploadMatrix(id, projectionUniform)
    }

    fun uploadProjectionMatrix(buffer: FloatBuffer) {
        glProgramUniformMatrix4(id, projectionUniform, false, buffer)
    }

    fun updateModelViewMatrix() {
        MatrixUtils.loadModelViewMatrix().uploadMatrix(id, modelViewUniform)
    }

    fun updateModelViewMatrix(matrix: Matrix4f) {
        MatrixUtils.loadMatrix(matrix).uploadMatrix(id, modelViewUniform)
    }

    fun uploadModelViewMatrix(buffer: FloatBuffer) {
        glProgramUniformMatrix4(id, modelViewUniform, false, buffer)
    }
}