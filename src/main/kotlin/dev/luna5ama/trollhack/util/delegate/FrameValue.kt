package dev.luna5ama.trollhack.util.delegate

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.event.AlwaysListening
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.listener
import kotlin.reflect.KProperty

class FrameValue<T>(private val block: () -> T) {
    private var value0: T? = null
    private var lastUpdateFrame = Int.MIN_VALUE

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
            listener<RunGameLoopEvent.Start>(Int.MAX_VALUE) {
                if (frame == Int.MAX_VALUE) {
                    frame = Int.MIN_VALUE
                    instances.forEach {
                        it.updateLazy()
                    }
                    return@listener
                }

                frame++
            }
        }
    }
}

fun interface FloatSupplier {
    fun get(): Float
}

class FrameFloat(private val block: FloatSupplier) {
    private var value0 = Float.NaN
    private var lastUpdateFrame = Int.MIN_VALUE

    val value: Float
        get() = get()

    init {
        instances.add(this)
    }

    fun get(): Float {
        return if (lastUpdateFrame == frame) {
            getLazy()
        } else {
            getForce()
        }
    }

    fun getLazy(): Float {
        var value = value0
        if (value.isNaN()) {
            value = getForce()
        }
        return value
    }

    fun getForce(): Float {
        val value = block.get()
        value0 = value
        lastUpdateFrame = frame

        return value
    }

    fun updateLazy() {
        value0 = Float.NaN
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Float {
        return get()
    }

    private companion object : AlwaysListening {
        val instances = ArrayList<FrameFloat>()
        val timer = TickTimer(TimeUnit.SECONDS)

        var frame = 0

        init {
            listener<RunGameLoopEvent.Start>(Int.MAX_VALUE) {
                if (frame == Int.MAX_VALUE) {
                    frame = Int.MIN_VALUE
                    instances.forEach {
                        it.updateLazy()
                    }
                    return@listener
                }

                frame++
            }
        }
    }
}