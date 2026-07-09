package dev.luna5ama.trollhack.graphics

import dev.luna5ama.trollhack.utils.timing.TickTimer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class BacksideFrameCounter(val framesTotal: Int, val fps: Int) : ReadOnlyProperty<Any?, Int> {
    private val delay = 1000 / fps
    private val timer = TickTimer()
    private var current = AtomicInteger(1)
    private var backside = false

    override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return getValue()
    }

    fun getValue(): Int {
        val value = if (timer.tickAndReset(delay))
            if (backside) current.getAndDecrement()
            else current.getAndIncrement()
        else current.get()
        if (value > framesTotal) {
            backside = true
            current.set(framesTotal - 1)
            return framesTotal - 1
        } else if (value < 1) {
            backside = false
            current.set(2)
            return 2
        }
        return value
    }
}