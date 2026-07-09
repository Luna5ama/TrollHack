package dev.luna5ama.trollhack.utils.delegates

import dev.luna5ama.trollhack.utils.extension.getValue
import dev.luna5ama.trollhack.utils.extension.setValue
import dev.luna5ama.trollhack.utils.threads.Coroutine
import dev.luna5ama.trollhack.utils.timing.TickTimer
import dev.luna5ama.trollhack.utils.timing.TimeUnit
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import kotlin.experimental.ExperimentalTypeInference
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.system.measureTimeMillis

/**
 * 假定每次更新所阻塞的时间不变，此类在updateTime <= latency的情况下可以保持阻塞时间仍然约为updateTime，适用于计算时�?更新时长的场�? * 但是获取结果为undefined
 *
 * @author SagiriXiguajerry
 */
@Deprecated("Undefined Behavior")
@OptIn(ExperimentalTypeInference::class)
class ParallelPropertyUpdater<T>(
    @BuilderInference private val invalidValue: T? = null,
    private val executor: ExecutorService = Coroutine.pool,
    @BuilderInference private val block: () -> T
) : Supplier<T>, ReadOnlyProperty<Any?, T> {
    private var value: T? by AtomicReference(null)
    private var isBeingUpdated by AtomicBoolean(false)
    private var lastLatency by AtomicLong(0)
    private val timer = TickTimer()

    init {
        executor.execute {
            lastLatency = measureTimeMillis {
                value = block()
            }
        }
    }

    fun get(updateTime: Int, timeUnit: TimeUnit): T {
        return get(updateTime * timeUnit.multiplier)
    }

    fun get(updateTime: Long, timeUnit: TimeUnit): T {
        return get(updateTime * timeUnit.multiplier)
    }

    fun get(updateTime: Int): T {
        return get(updateTime.toLong())
    }

    fun get(updateTime: Long): T {
        var cached = value

        return if (cached == null || cached == invalidValue || timer.tick(updateTime)) {
            timer.reset()
            if (!isBeingUpdated) {
                executor.submit {
                    isBeingUpdated = true
                    lastLatency = measureTimeMillis {
                        value = block()
                    }
                    isBeingUpdated = false
                }
            }
            while (true) {
                if (value != null) {
                    cached = value
                    break
                }
            }
            if (lastLatency > updateTime) {
                executor.submit {
                    isBeingUpdated = true
                    lastLatency = measureTimeMillis {
                        value = block()
                    }
                    timer.reset()
                    isBeingUpdated = false
                }
            }
            cached!!
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

    override fun get(): T {
        return getForce()
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return getForce()
    }
}