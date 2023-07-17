package dev.luna5ama.trollhack.graphics

import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.glVertexAttribIPointer
import org.lwjgl.opengl.GL33.glVertexAttribDivisor

class VertexAttribute private constructor(private val stride: Int, private val entries: List<Entry>) {
    fun apply() {
        entries.forEach {
            it.apply(stride)
        }
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

        fun apply(stride: Int)
    }

    private class FloatEntry(
        override val index: Int,
        override val size: Int,
        override val type: Int,
        override val pointer: Int,
        val normalized: Boolean,
        override val divisor: Int
    ) : Entry {
        override fun apply(stride: Int) {
            glVertexAttribPointer(index, size, type, normalized, stride, pointer.toLong())

            glEnableVertexAttribArray(index)
            if (divisor != 0) {
                glVertexAttribDivisor(index, divisor)
            }
        }
    }

    private open class IntEntry(
        override val index: Int,
        override val size: Int,
        override val type: Int,
        override val pointer: Int,
        override val divisor: Int
    ) : Entry {

        override fun apply(stride: Int) {
            glVertexAttribIPointer(index, size, type, stride, pointer.toLong())

            glEnableVertexAttribArray(index)
            if (divisor != 0) {
                glVertexAttribDivisor(index, divisor)
            }
        }
    }
}


inline fun buildAttribute(stride: Int, block: VertexAttribute.Builder.() -> Unit): VertexAttribute {
    return VertexAttribute.Builder(stride).apply(block).build()
}