package dev.luna5ama.trollhack.graphics.buffer

import dev.luna5ama.kmogus.asMutable
import dev.luna5ama.trollhack.event.AlwaysListening
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.graphics.*
import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL32.*
import org.lwjgl.opengl.GL44.GL_MAP_COHERENT_BIT
import org.lwjgl.opengl.GL44.GL_MAP_PERSISTENT_BIT
import org.lwjgl.opengl.GL45.glCreateBuffers
import org.lwjgl.opengl.GL45.glCreateVertexArrays

object PersistentMappedVBO : AlwaysListening {
    private val vbo = glCreateBuffers().apply {
        glNamedBufferStorage(
            this,
            64L * 1024L * 1024L,
            0,
            GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT
        )
    }
    val arr = glMapNamedBufferRange(
        vbo,
        0,
        64L * 1024L * 1024L,
        GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT or GL_MAP_UNSYNCHRONIZED_BIT
    ).asMutable()
    var drawOffset = 0

    private var sync = 0L

    fun end() {
        drawOffset = (arr.pos / 16L).toInt()
    }

    init {
        listener<RunGameLoopEvent.End> {
            if (sync == 0L) {
                if (arr.pos >= arr.len / 2) {
                    @Suppress("RemoveRedundantQualifierName", "RedundantSuppression")
                    sync = dev.luna5ama.trollhack.graphics.glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0)
                }
            } else if (glGetSynciv(sync, GL_SYNC_STATUS) == GL_SIGNALED) {
                glDeleteSync(sync)
                sync = 0L
                arr.pos = 0L
                drawOffset = 0
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