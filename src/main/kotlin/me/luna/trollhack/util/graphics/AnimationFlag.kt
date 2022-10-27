package me.luna.trollhack.util.graphics

class AnimationFlag(private val interpolation: InterpolateFunction) {

    constructor(easing: Easing, length: Float) : this({ time, prev, current ->
        easing.incOrDec(Easing.toDelta(time, length), prev, current)
    })

    var prev = 0.0f; private set
    var current = 0.0f; private set
    var time = System.currentTimeMillis(); private set

    fun forceUpdate(prev: Float, current: Float) {
        if (prev.isNaN() || current.isNaN()) return

        this.prev = prev
        this.current = current
        time = System.currentTimeMillis()
    }

    fun update(current: Float) {
        if (!current.isNaN() && this.current != current) {
            prev = this.current
            this.current = current
            time = System.currentTimeMillis()
        }
    }

    fun getAndUpdate(current: Float): Float {
        val render = interpolation.invoke(time, prev, this.current)

        if (!current.isNaN() && current != this.current) {
            prev = render
            this.current = current
            time = System.currentTimeMillis()
        }

        return render
    }
}