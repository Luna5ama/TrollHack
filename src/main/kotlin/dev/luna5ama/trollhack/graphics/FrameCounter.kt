package dev.luna5ama.trollhack.graphics

import dev.luna5ama.trollhack.utils.timing.TickTimer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class FrameCounter(val framesTotal: Int, val fps: Int) : ReadOnlyProperty<Any?, Int> {
    private val delay = 1000 / fps
    private val timer = TickTimer()
    private var current = AtomicInteger(1)

    override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return getValue()
    }

    fun getValue(): Int {
        val value =  if (timer.tickAndReset(delay)) current.incrementAndGet()
        else current.get()
        if (value > framesTotal) {
            current.set(1)
            return 1
        }
        return value
    }
}