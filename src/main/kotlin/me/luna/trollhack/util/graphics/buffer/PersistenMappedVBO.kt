package me.luna.trollhack.util.graphics.buffer

import me.luna.trollhack.event.AlwaysListening
import me.luna.trollhack.event.events.RunGameLoopEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.util.graphics.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL44.GL_MAP_COHERENT_BIT
import org.lwjgl.opengl.GL44.GL_MAP_PERSISTENT_BIT
import org.lwjgl.opengl.GL45.glCreateBuffers
import org.lwjgl.opengl.GL45.glCreateVertexArrays

object PersistenMappedVBO : AlwaysListening {
    val vbo = glCreateBuffers().apply {
        glNamedBufferStorage(
            this,
            64L * 1024L * 1024L,
            0,
            GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT
        )
    }
    val array = glMapNamedBufferRange(
        vbo,
        0,
        64L * 1024L * 1024L,
        GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT or GL_MAP_UNSYNCHRONIZED_BIT
    )
    var drawOffset = 0

    private var frameCounter = 1

    fun end() {
        drawOffset = (array.pointer / 16L).toInt()
    }

    init {
        listener<RunGameLoopEvent.Start> {
            if (++frameCounter == 3) {
                array.pointer = 0L
                drawOffset = 0
                frameCounter = 0
            }
        }
    }

    val POS2_COLOR = createVao(buildAttribute(16) {
        float(0, 2, GLDataType.GL_FLOAT, false)
        float(1, 4, GLDataType.GL_UNSIGNED_BYTE, true)
    })

    val POS3_COLOR = createVao(buildAttribute(16) {
        float(0, 3, GLDataType.GL_FLOAT, false)
        float(1, 4, GLDataType.GL_UNSIGNED_BYTE, true)
    })

    fun createVao(vertexAttribute: VertexAttribute): Int {
        val vaoID = glCreateVertexArrays()
        glBindVertexArray(vaoID)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        vertexAttribute.apply()
        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        return vaoID
    }
}