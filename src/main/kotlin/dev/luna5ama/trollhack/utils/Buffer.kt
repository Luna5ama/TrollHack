package dev.luna5ama.trollhack.utils

import org.lwjgl.system.MemoryStack
import java.nio.*

inline val memStack get() = MemoryStack.stackPush()

fun mallocInt(size: Int): IntBuffer = memStack.mallocInt(size)

fun mallocFloat(size: Int): FloatBuffer = memStack.mallocFloat(size)

fun mallocDouble(size: Int): DoubleBuffer = memStack.mallocDouble(size)

fun createDirectByteBuffer(capacity: Int): ByteBuffer {
    return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder())
}

fun createDirectIntBuffer(capacity: Int): IntBuffer {
    return createDirectByteBuffer(capacity shl 2).asIntBuffer()
}

fun createDirectFloatBuffer(capacity: Int): FloatBuffer {
    return createDirectByteBuffer(capacity shl 2).asFloatBuffer()
}

fun createDirectDoubleBuffer(capacity: Int): DoubleBuffer {
    return createDirectByteBuffer(capacity shl 4).asDoubleBuffer()
}

fun createDirectLongBuffer(capacity: Int): LongBuffer {
    return createDirectByteBuffer(capacity shl 4).asLongBuffer()
}