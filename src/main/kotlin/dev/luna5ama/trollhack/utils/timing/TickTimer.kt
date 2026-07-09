package dev.luna5ama.trollhack.utils.timing

class TickTimer(val timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
    @Volatile
    var time = System.currentTimeMillis()
    val passed get() = System.currentTimeMillis() - time

    constructor(timeUnit: java.util.concurrent.TimeUnit) : this(TimeUnit.from(timeUnit))

    fun tick(delay: Int): Boolean {
        val current = System.currentTimeMillis()
        return current - time >= delay * timeUnit.multiplier
    }

    fun tick(delay: Long): Boolean {
        val current = System.currentTimeMillis()
        return current - time >= delay * timeUnit.multiplier
    }

    fun tick(delay: Int, unit: TimeUnit): Boolean {
        val current = System.currentTimeMillis()
        return current - time >= delay * unit.multiplier
    }

    fun tick(delay: Long, unit: TimeUnit): Boolean {
        val current = System.currentTimeMillis()
        return current - time >= delay * unit.multiplier
    }

    fun tickAndReset(delay: Int): Boolean {
        val current = System.currentTimeMillis()
        return if (current - time >= delay * timeUnit.multiplier) {
            time = current
            true
        } else {
            false
        }
    }

    fun tickAndReset(delay: Long): Boolean {
        val current = System.currentTimeMillis()
        return if (current - time >= delay * timeUnit.multiplier) {
            time = current
            true
        } else {
            false
        }
    }

    fun tickAndReset(delay: Int, unit: TimeUnit): Boolean {
        val current = System.currentTimeMillis()
        return if (current - time >= delay * unit.multiplier) {
            time = current
            true
        } else {
            false
        }
    }

    fun tickAndReset(delay: Long, unit: TimeUnit): Boolean {
        val current = System.currentTimeMillis()
        return if (current - time >= delay * unit.multiplier) {
            time = current
            true
        } else {
            false
        }
    }

    fun reset() {
        time = System.currentTimeMillis()
    }

    fun reset(offset: Long) {
        time = System.currentTimeMillis() + offset
    }

    fun reset(offset: Int) {
        time = System.currentTimeMillis() + offset
    }
}