package dev.luna5ama.trollhack.utils

object TimerUtil {
    var timer: Float = 1f

    fun set(factor: Float) {
        var factor = factor
        if (factor < 0.1f) factor = 0.1f
        timer = factor
    }

    var lastTime: Float = 0f

    fun reset() {
        timer = default
        lastTime = timer
    }

    fun tryReset() {
        if (lastTime != default) {
            reset()
        }
    }

    fun passedS(s: Double): Boolean {
        return passedMs(s.toLong() * 1000L)
    }

    fun passedMs(ms: Long): Boolean {
        return passedNS(convertToNS(ms))
    }

    fun passedMs(ms: Double): Boolean {
        return passedMs(ms.toLong())
    }

    fun passed(ms: Long): Boolean {
        return passedNS(convertToNS(ms))
    }

    fun passed(ms: Double): Boolean {
        return passedMs(ms.toLong())
    }

    fun setMs(ms: Long) {
        timer = (System.nanoTime() - convertToNS(ms)).toFloat()
    }

    fun passedNS(ns: Long): Boolean {
        return System.nanoTime() - timer >= ns
    }

    val passedTimeMs: Long
        get() = getMs((System.nanoTime() - timer).toLong())

    fun getMs(time: Long): Long {
        return time / 1000000L
    }

    fun convertToNS(time: Long): Long {
        return time * 1000000L
    }

    fun get(): Float {
        return timer
    }

    val default = 1f
}

