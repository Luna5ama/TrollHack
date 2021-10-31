package cum.xiaro.trollhack.util.delegate

import cum.xiaro.trollhack.event.AlwaysListening
import cum.xiaro.trollhack.event.events.RunGameLoopEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.TimeUnit
import kotlin.reflect.KProperty

class FrameValue<T>(private val block: () -> T) {
    private var value0: T? = null
    private var lastUpdateFrame = 0

    val value: T
        get() = get()

    init {
        instances.add(this)
    }

    fun get(): T {
        return if (lastUpdateFrame == frame) {
            getLazy()
        } else {
            getForce()
        }
    }

    fun getLazy(): T {
        return value0 ?: getForce()
    }

    fun getForce(): T {
        val value = block.invoke()
        value0 = value
        lastUpdateFrame = frame

        return value
    }

    fun updateLazy() {
        value0 = null
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return get()
    }

    private companion object : AlwaysListening {
        val instances = ArrayList<FrameValue<*>>()
        val timer = TickTimer(TimeUnit.SECONDS)

        var frame = 0

        init {
            listener<RunGameLoopEvent.Render> {
                frame++

                if (timer.tick(1L)) {
                    frame = 0
                    instances.forEach {
                        it.updateLazy()
                    }
                }
            }
        }
    }
}