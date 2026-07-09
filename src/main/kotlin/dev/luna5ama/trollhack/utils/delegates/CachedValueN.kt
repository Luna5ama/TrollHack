package dev.luna5ama.trollhack.utils.delegates

import dev.luna5ama.trollhack.utils.timing.TickTimer
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class CachedValueN<T>(
    private val updateTime: Long,
    private val invalidValue: T? = null,
    private val block: () -> T
) : ReadWriteProperty<Any?, T>, PropertyDelegateProvider<Any?, CachedValueN<T>> {
    protected var value: T? = null
    protected val timer = TickTimer()

    open fun get(): T {
        return get(updateTime)
    }

    fun get(updateTime: Int): T {
        val cached = value

        return if (cached == null || cached == invalidValue || timer.tick(updateTime)) {
            timer.reset()
            block().also { value = it }
        } else {
            cached
        }
    }

    fun get(updateTime: Long): T {
        val cached = value

        return if (cached == null || cached == invalidValue || timer.tick(updateTime)) {
            timer.reset()
            block().also { value = it }
        } else {
            cached
        }
    }

    fun getForce(): T {
        timer.reset()
        return block().also { value = it }
    }

    fun getLazy(): T? {
        return value
    }

    fun updateForce() {
        timer.reset()
        value = block()
    }

    fun updateLazy() {
        value = null
    }

    override operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): CachedValueN<T> {
        return this
    }

    override operator fun getValue(thisRef: Any?, property: KProperty<*>) = get()

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}