package cum.xiaro.trollhack.util.delegate

import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.TimeUnit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class CachedValue<T>(
    private val invalidValue: T? = null,
    private val block: () -> T
) {
    private var value: T? = null
    private val timer = TickTimer()

    fun get(updateTime: Int, timeUnit: TimeUnit): T {
        return get(updateTime * timeUnit.multiplier)
    }

    fun get(updateTime: Long, timeUnit: TimeUnit): T {
        return get(updateTime * timeUnit.multiplier)
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

    fun wrapped(updateTime: Long): ReadWriteProperty<Any?, T> {
        return object : ReadWriteProperty<Any?, T> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return this@CachedValue.get(updateTime)
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                this@CachedValue.value = value
            }
        }
    }

    fun wrapped(updateTime: Long, timeUnit: TimeUnit): ReadWriteProperty<Any?, T> {
        return object : ReadWriteProperty<Any?, T> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return this@CachedValue.get(updateTime, timeUnit)
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                this@CachedValue.value = value
            }
        }
    }
}