package cum.xiaro.trollhack.util.graphics.fastrender.model

import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glVertexAttribIPointer

abstract class Model(private val textureSizeX: Int, private val textureSizeZ: Int) {
    private var vboID = 0
    var modelSize = 0; private set

    fun init() {
        val builder = ModelBuilder(0, textureSizeX, textureSizeZ)
        builder.buildModel()

        vboID = glGenBuffers()
        modelSize = builder.vertexSize

        glBindBuffer(GL_ARRAY_BUFFER, vboID)
        glBufferData(GL_ARRAY_BUFFER, builder.build(), GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    protected abstract fun ModelBuilder.buildModel()

    fun attachVBO() {
        glBindBuffer(GL_ARRAY_BUFFER, vboID)

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 20, 0L) // 12
        glVertexAttribPointer(1, 2, GL_UNSIGNED_SHORT, true, 20, 12L) // 4
        glVertexAttribPointer(2, 3, GL_BYTE, false, 20, 16L) // 3
        glVertexAttribIPointer(3, 1, GL_UNSIGNED_BYTE, 20, 19L) // 1

        glEnableVertexAttribArray(0)
        glEnableVertexAttribArray(1)
        glEnableVertexAttribArray(2)
        glEnableVertexAttribArray(3)
    }
}