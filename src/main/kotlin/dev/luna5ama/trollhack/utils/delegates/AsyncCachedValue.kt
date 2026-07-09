package dev.luna5ama.trollhack.utils.delegates

import kotlinx.coroutines.launch
import dev.luna5ama.trollhack.utils.threads.Coroutine
import dev.luna5ama.trollhack.utils.timing.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.properties.ReadWriteProperty

class AsyncCachedValue<T>(
    updateTime: Long,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
    private val context: CoroutineContext = Coroutine.context,
    block: () -> T
) : CachedValue<T>(updateTime, timeUnit, block), ReadWriteProperty<Any?, T> {

    override fun get(): T {
        val cached = value

        return when {
            cached == null -> {
                block().also { value = it }
            }
            timer.tickAndReset(updateTime) -> {
                Coroutine.launch(context) {
                    value = block()
                }
                cached
            }
            else -> {
                cached
            }
        }
    }

    override fun update() {
        timer.reset()
        Coroutine.launch(context) {
            value = block()
        }
    }
}