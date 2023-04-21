package dev.luna5ama.trollhack.util.state

open class TimedFlag<T>(value: T) {
    var value = value
        set(value) {
            if (value != field) {
                lastUpdateTime = System.currentTimeMillis()
                field = value
            }
        }

    var lastUpdateTime = System.currentTimeMillis()
        private set

    fun resetTime() {
        lastUpdateTime = System.currentTimeMillis()
    }
}