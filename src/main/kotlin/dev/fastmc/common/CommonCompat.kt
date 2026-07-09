@file:Suppress("unused", "nothing_to_inline")

package dev.fastmc.common

import kotlinx.coroutines.Job
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Supplier
import kotlin.math.PI
import kotlin.math.floor

typealias DoubleBuffered<T> = dev.luna5ama.trollhack.utils.DoubleBuffered<T>

inline fun Double.floorToInt(): Int = floor(this).toInt()
inline fun Float.floorToInt(): Int = floor(this).toInt()

inline fun Double.toDegree(): Double = this * 180.0 / PI
inline fun Float.toDegree(): Float = (this * 180.0f / PI.toFloat())

inline fun Double.toRadians(): Double = this / 180.0 * PI
inline fun Float.toRadians(): Float = (this / 180.0f * PI.toFloat())

val Job?.isCompletedOrNull: Boolean
    get() = this == null || this.isCompleted

fun wrapDirectByteBuffer(address: Long, length: Int): ByteBuffer {
    return MemoryUtil.memByteBuffer(address, length)
}

class ConcurrentObjectPool<T>(private val supplier: Supplier<T>) {
    constructor(supplier: () -> T) : this(Supplier(supplier))

    private val queue = ConcurrentLinkedQueue<T>()

    fun get(): T = queue.poll() ?: supplier.get()

    fun put(value: T) {
        queue.offer(value)
    }
}
