package dev.luna5ama.trollhack.utils.timing

class NanoTickTimer(val timeUnit: NanoTimeUnit = NanoTimeUnit.Millisecond) {

    private var time = System.nanoTime()

    fun passed(interval: Int, timeUnit: NanoTimeUnit = this.timeUnit): Boolean {
        return System.nanoTime() - time > interval * timeUnit.multiplier
    }

    fun reset() {
        time = System.nanoTime()
    }

    fun passedAndReset(interval: Int, timeUnit: NanoTimeUnit = this.timeUnit): Boolean {
        val result = passed(interval, timeUnit)
        if (result) reset()
        return result
    }

    inline fun passedAndReset(interval: Int, timeUnit: NanoTimeUnit = this.timeUnit, block: () -> Unit): Boolean {
        val result = passed(interval, timeUnit)
        if (result) {
            reset()
            block()
        }
        return result
    }

    inline fun passedAndRun(
        interval: Int,
        timeUnit: NanoTimeUnit = this.timeUnit,
        reset: Boolean = false,
        block: () -> Unit
    ): Boolean {
        val result = passed(interval, timeUnit)
        if (result) {
            block()
            if (reset) reset()
        }
        return result
    }

    private var offset = 0L

    // Tick per second
    fun tps(tps: Int, block: () -> Unit) {
        val currentNanoTime = System.nanoTime()
        val delayNanoTime = ((1000000000.0 / tps).toLong() - offset).coerceAtLeast(0)
        val timeLapsed = currentNanoTime - time
        if (timeLapsed >= delayNanoTime) {
            offset = timeLapsed - delayNanoTime
            time = currentNanoTime
            block()
        }
    }

    fun tps(tps: Int, block: () -> Unit, elseBlock: (() -> Unit)) {
        val currentNanoTime = System.nanoTime()
        val delayNanoTime = ((1000000000.0 / tps).toLong() - offset).coerceAtLeast(0)
        val timeLapsed = currentNanoTime - time
        if (timeLapsed >= delayNanoTime) {
            offset = timeLapsed - delayNanoTime
            time = currentNanoTime
            block()
        } else elseBlock.invoke()
    }

}