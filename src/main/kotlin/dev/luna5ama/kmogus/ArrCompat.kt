@file:Suppress("unused")

package dev.luna5ama.kmogus

import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import java.nio.ByteBuffer

class Arr private constructor(private val buffer: ByteBuffer) {
    var pos: Long = 0L

    val len: Long
        get() = buffer.capacity().toLong()

    val ptr: Pointer
        get() = Pointer(buffer, pos.toInt())

    operator fun plusAssign(bytes: Int) {
        pos += bytes.toLong()
    }

    companion object {
        fun wrap(buffer: ByteBuffer): Arr = Arr(buffer)
    }

    class Pointer(private val buffer: ByteBuffer, private val base: Int) {
        operator fun set(offset: Int, value: Float) {
            buffer.putFloat(base + offset, value)
        }

        operator fun set(offset: Int, value: Int) {
            buffer.putInt(base + offset, value)
        }

        operator fun set(offset: Int, value: ColorRGBA) {
            buffer.putInt(base + offset, value.rgba)
        }
    }
}

fun Arr.asMutable(): Arr = this
