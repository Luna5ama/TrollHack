package cum.xiaro.trollhack.util.graphics.color

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.util.math.MathUtils
import kotlin.math.max
import kotlin.math.roundToInt

class ColorGradient(vararg stops: Stop) {
    private val colorArray = stops.apply { sortBy { it.value } }

    fun get(valueIn: Float): ColorRGB {
        if (colorArray.isEmpty()) return ColorRGB(255, 255, 255)
        var prevStop = colorArray.last()
        var nextStop = colorArray.last()
        for ((index, pair) in colorArray.withIndex()) {
            if (pair.value < valueIn) continue
            prevStop = if (pair.value == valueIn) pair
            else colorArray[max(index - 1, 0)]
            nextStop = pair
            break
        }
        if (prevStop == nextStop) return prevStop.color
        val r = MathUtils.convertRange(valueIn, prevStop.value, nextStop.value, prevStop.color.r.toFloat(), nextStop.color.r.toFloat()).roundToInt()
        val g = MathUtils.convertRange(valueIn, prevStop.value, nextStop.value, prevStop.color.g.toFloat(), nextStop.color.g.toFloat()).roundToInt()
        val b = MathUtils.convertRange(valueIn, prevStop.value, nextStop.value, prevStop.color.b.toFloat(), nextStop.color.b.toFloat()).roundToInt()
        val a = MathUtils.convertRange(valueIn, prevStop.value, nextStop.value, prevStop.color.a.toFloat(), nextStop.color.a.toFloat()).roundToInt()
        return ColorRGB(r, g, b, a)
    }

    class Stop(val value: Float, val color: ColorRGB)
}