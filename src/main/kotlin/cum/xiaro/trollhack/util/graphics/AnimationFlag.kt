package cum.xiaro.trollhack.util.graphics

class AnimationFlag(private val interpolation: InterpolateFunction) {

    constructor(easing: Easing, length: Float) : this({ time, prev, current ->
        easing.incOrDec(Easing.toDelta(time, length), prev, current)
    })

    private var prev = 0.0f
    private var current = 0.0f
    private var time = System.currentTimeMillis()

    fun forceUpdate(prev: Float, current: Float) {
        this.prev = prev
        this.current = current
        time = System.currentTimeMillis()
    }

    fun getAndUpdate(input: Float): Float {
        val render = interpolation.invoke(time, prev, current)

        if (input != current) {
            prev = render
            current = input
            time = System.currentTimeMillis()
        }

        return render
    }

    fun get(input: Float, update: Boolean): Float {
        val render = interpolation.invoke(time, prev, current)

        if (update && input != current) {
            prev = render
            current = input
            time = System.currentTimeMillis()
        }

        return render
    }
}