package dev.luna5ama.trollhack.utils

import java.util.function.Consumer
import java.util.function.Supplier

class DoubleBuffered<T>(supplier: Supplier<T>, private val initAction: Consumer<in T>) {
    @Suppress("UNCHECKED_CAST")
    constructor(supplier: Supplier<T>) : this(supplier, DEFAULT_INIT_ACTION as Consumer<T>)

    @Volatile
    var front = supplier.get(); private set

    @Volatile
    var back = supplier.get(); private set

    fun swap(): DoubleBuffered<T> {
        val temp = front
        front = back
        back = temp
        return this
    }

    fun initFrontBack(): DoubleBuffered<T> {
        initAction.accept(front)
        initAction.accept(back)
        return this
    }

    fun initFront(): DoubleBuffered<T> {
        initAction.accept(front)
        return this
    }

    fun initBack(): DoubleBuffered<T> {
        initAction.accept(back)
        return this
    }

    companion object {
        @JvmField
        val DEFAULT_INIT_ACTION = Consumer<Any?> {
        }

        @JvmField
        val CLEAR_INIT_ACTION = Consumer<MutableCollection<*>> {
            it.clear()
        }
    }
}