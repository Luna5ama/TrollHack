package cum.xiaro.trollhack.util.state

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
}