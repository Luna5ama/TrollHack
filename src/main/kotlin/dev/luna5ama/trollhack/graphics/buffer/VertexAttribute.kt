package dev.luna5ama.trollhack.graphics.buffer

import dev.luna5ama.trollhack.graphics.GLDataType
import org.lwjgl.opengl.GL45.*

class VertexAttribute private constructor(val stride: Int, private val entries: List<Entry>) {
    fun apply(vao: Int, bufferIndex: Int) = entries.forEach {
        it.apply(vao, bufferIndex)
    }

    class Builder(private val stride: Int) {
        private var pointer = 0
        private val entries = ArrayList<Entry>()

        fun int(index: Int, size: Int, type: GLDataType, divisor: Int = 0) {
            entries.add(IntEntry(index, size, type.glEnum, pointer, divisor))
            pointer += size * type.size
        }

        fun float(index: Int, size: Int, type: GLDataType, normalized: Boolean, divisor: Int = 0) {
            entries.add(FloatEntry(index, size, type.glEnum, pointer, normalized, divisor))
            pointer += size * type.size
        }

        fun build(): VertexAttribute {
            return VertexAttribute(stride, entries)
        }
    }

    private sealed interface Entry {
        val index: Int
        val size: Int
        val type: Int
        val pointer: Int
        val divisor: Int

        fun apply(vao: Int, bufferIndex: Int)
    }

    private class FloatEntry(
        override val index: Int,
        override val size: Int,
        override val type: Int,
        override val pointer: Int,
        val normalized: Boolean,
        override val divisor: Int
    ) : Entry {
        override fun apply(vao: Int, bufferIndex: Int) {
            glVertexArrayAttribBinding(vao, index, bufferIndex)
            glEnableVertexArrayAttrib(vao, index)
            glVertexArrayAttribFormat(vao, index, size, type, normalized, pointer)

            if (divisor != 0) {
                glVertexArrayBindingDivisor(vao, index, divisor)
            }
        }
    }

    private class IntEntry(
        override val index: Int,
        override val size: Int,
        override val type: Int,
        override val pointer: Int,
        override val divisor: Int
    ) : Entry {
        override fun apply(vao: Int, bufferIndex: Int) {
            glVertexArrayAttribBinding(vao, index, bufferIndex)
            glEnableVertexArrayAttrib(vao, index)
            glVertexArrayAttribIFormat(vao, index, size, type, pointer)

            if (divisor != 0) {
                glVertexArrayBindingDivisor(vao, index, divisor)
            }
        }
    }
}

inline fun buildAttribute(stride: Int, block: VertexAttribute.Builder.() -> Unit): VertexAttribute {
    return VertexAttribute.Builder(stride).apply(block).build()
}