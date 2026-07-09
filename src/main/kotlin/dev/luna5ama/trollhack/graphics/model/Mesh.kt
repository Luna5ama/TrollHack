package dev.luna5ama.trollhack.graphics.model

import com.mojang.blaze3d.opengl.GlConst.GL_ARRAY_BUFFER
import com.mojang.blaze3d.opengl.GlConst.GL_STATIC_DRAW
import dev.luna5ama.trollhack.utils.createDirectByteBuffer
import dev.luna5ama.trollhack.graphics.GLHelper
import dev.luna5ama.trollhack.graphics.matrix.MatrixLayerStack
import org.lwjgl.opengl.ARBDirectStateAccess
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL45.*

abstract class Mesh(
    val vertices: MutableList<Vertex>,
    val diffuse: MutableList<ModelTexture>,
    val normal: MutableList<ModelTexture>,
    val specular: MutableList<ModelTexture>,
    val height: MutableList<ModelTexture>,
    val indices: IntArray,
    val primitives: Int
) {
    // Nvidia GeForce 6000/7000 series supports VAO via ARB
    val vao = glCreateVertexArrays()

    // Textures
    val diffuseTexture = diffuse.getOrNull(0)
    val normalTexture = normal.getOrNull(0)
    val specularTexture = specular.getOrNull(0)
    val heightTexture = height.getOrNull(0)
    val textures = diffuse.toMutableList().apply {
        addAll(normal)
        addAll(specular)
        addAll(height)
        forEach {
            it.texture.bindTexture()
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        }
    }

    open fun setupMesh() {
        val buffer = createDirectByteBuffer(vertices.size * 32).apply {
            vertices.forEach {
                putFloat(it.position.x)
                putFloat(it.position.y)
                putFloat(it.position.z)
                putFloat(it.texCoords.x)
                putFloat(it.texCoords.y)
                putFloat(it.normal.x)
                putFloat(it.normal.y)
                putFloat(it.normal.z)
            }
            flip()
        }

        val indicesBuffer = createDirectByteBuffer(indices.size * 4).apply {
            indices.forEach {
                putInt(it)
            }
            flip()
        }
        val vbo = glCreateBuffers()
        val ibo = glCreateBuffers()

        glVertexArrayVertexBuffer(vao, 0, vbo, 0, 32)
        glVertexArrayElementBuffer(vao, ibo)

        glNamedBufferData(vbo, buffer, GL_STATIC_DRAW)
        glNamedBufferData(ibo, indicesBuffer, GL_STATIC_DRAW)

        // Position
        glVertexArrayAttribFormat(vao, 0, 3, GL_FLOAT, false, 0)
        glVertexArrayAttribBinding(vao, 0, 0)
        glEnableVertexArrayAttrib(vao, 0)

        // TexCoords
        glVertexArrayAttribFormat(vao, 1, 2, GL_FLOAT, false, 12)
        glVertexArrayAttribBinding(vao, 1, 0)
        glEnableVertexArrayAttrib(vao, 1)

        // Normal
        glVertexArrayAttribFormat(vao, 2, 2, GL_FLOAT, false, 20)
        glVertexArrayAttribBinding(vao, 2, 0)
        glEnableVertexArrayAttrib(vao, 2)
    }

    abstract fun MatrixLayerStack.MatrixScope.draw()

}

data class MeshData(
    val vertices: MutableList<Vertex>,
    val diffuse: MutableList<ModelTexture>,
    val normal: MutableList<ModelTexture>,
    val specular: MutableList<ModelTexture>,
    val height: MutableList<ModelTexture>,
    val indices: IntArray,
    val primitives: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MeshData

        if (vertices != other.vertices) return false
        if (diffuse != other.diffuse) return false
        if (normal != other.normal) return false
        if (specular != other.specular) return false
        if (height != other.height) return false
        if (primitives != other.primitives) return false
        return indices.contentEquals(other.indices)
    }

    override fun hashCode(): Int {
        var result = vertices.hashCode()
        result = 31 * result + diffuse.hashCode()
        result = 31 * result + normal.hashCode()
        result = 31 * result + specular.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + primitives.hashCode()
        result = 31 * result + indices.contentHashCode()
        return result
    }
}