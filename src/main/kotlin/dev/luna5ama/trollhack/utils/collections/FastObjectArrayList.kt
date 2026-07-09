package dev.luna5ama.trollhack.utils.collections

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrays
import kotlin.math.max

class FastObjectArrayList<E> : ObjectArrayList<E> {
    private constructor(array: Array<E>, dummy: Boolean) : super(array, dummy)
    constructor() : super(20)
    constructor(c: Collection<E>) : super(c)
    constructor(capacity: Int) : super(capacity)

    val capacity: Int
        get() = this.a.size

    fun addAll(other: ObjectArrayList<E>) {
        if (other.isEmpty) return
        val newSize = this.size + other.size
        this.ensureCapacity(newSize)
        System.arraycopy(other.elements(), 0, this.a, this.size, other.size)
        this.size = newSize
    }

    fun addAll(other: Array<E>) {
        if (other.isEmpty()) return
        val newSize = this.size + other.size
        this.ensureCapacity(newSize)
        System.arraycopy(other, 0, this.a, this.size, other.size)
        this.size = newSize
    }

    fun clearAndTrim() {
        size = 0
        @Suppress("UNCHECKED_CAST")
        a = if (wrapped) {
            ObjectArrays.trim(a, 0)
        } else {
            emptyArray<Any?>() as Array<out E>
        }
    }

    override fun trim(n: Int) {
        if (n >= a.size || size == a.size) return
        a = if (wrapped) {
            ObjectArrays.trim(a, max(n, size))
        } else {
            @Suppress("UNCHECKED_CAST")
            val t = arrayOfNulls<Any?>(max(n, size)) as Array<out E>
            System.arraycopy(a, 0, t, 0, size)
            t
        }
    }

    fun clearFast() {
        size = 0
    }

    fun setSize(i: Int) {
        size = i
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <E> wrap(a: Array<E?>, length: Int): FastObjectArrayList<E> {
            require(length <= a.size) { "The specified length (" + length + ") is greater than the array size (" + a.size + ")" }
            val l = FastObjectArrayList(a, false)
            l.size = length
            return l as FastObjectArrayList<E>
        }

        @JvmStatic
        fun <E> wrap(a: Array<E?>): FastObjectArrayList<E> {
            return wrap(a, a.size)
        }

        @JvmStatic
        inline fun <reified E> typed(capacity: Int): FastObjectArrayList<E> {
            return wrap(arrayOfNulls(capacity), 0)
        }

        @JvmStatic
        inline fun <reified E> typed(): FastObjectArrayList<E> {
            return typed(DEFAULT_INITIAL_CAPACITY)
        }
    }
}