package dev.luna5ama.trollhack.graphics.buffer.pmvbo

import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.asMutable
import dev.luna5ama.trollhack.graphics.buffer.VertexAttribute
import org.lwjgl.opengl.*
import org.lwjgl.opengl.GL45.*
import java.nio.ByteBuffer

object PersistentMappedVBO {
    private val vbo = glCreateBuffers().apply {
        glNamedBufferStorage(
            this,
            64L * 1024L * 1024L,
            GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT
        )
    }
    val arr = Arr.wrap(
        glMapNamedBufferRange(
            vbo,
            0,
            64L * 1024L * 1024L,
            GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT or GL_MAP_UNSYNCHRONIZED_BIT
        ) as ByteBuffer
    ).asMutable()

    var drawOffset = 0
    private var sync = 0L

    fun end(stride: Int) {
        drawOffset = (arr.pos / stride).toInt()
    }

    fun onSync() {
        if (sync == 0L) {
            if (arr.pos >= arr.len / 2) {
                sync = GL32C.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0)
            }
        } else if (IntArray(1).apply {
                GL32C.glGetSynciv(
                    sync,
                    GL32.GL_SYNC_STATUS,
                    IntArray(1),
                    this
                )
            }[0] == GL32.GL_SIGNALED) {
            GL32C.glDeleteSync(sync)
            sync = 0L
            arr.pos = 0L
            drawOffset = 0
        }
    }

    fun createVao(vertexAttribute: VertexAttribute): Int {
        val vaoID = glCreateVertexArrays()
        glVertexArrayVertexBuffer(vaoID, 0, vbo, 0L, vertexAttribute.stride)
        vertexAttribute.apply(vaoID, 0)
        return vaoID
    }

}