package cum.xiaro.trollhack.util.math

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

@Suppress("NOTHING_TO_INLINE")
object MathUtils {
    @JvmStatic
    inline fun ceilToPOT(valueIn: Int): Int {
        // Magical bit shifting
        var i = valueIn
        i--
        i = i or (i shr 1)
        i = i or (i shr 2)
        i = i or (i shr 4)
        i = i or (i shr 8)
        i = i or (i shr 16)
        i++
        return i
    }

    @JvmStatic
    inline fun round(value: Float, places: Int): Float {
        val scale = 10.0f.pow(places)
        return round(value * scale) / scale
    }

    @JvmStatic
    inline fun round(value: Double, places: Int): Double {
        val scale = 10.0.pow(places)
        return round(value * scale) / scale
    }

    @JvmStatic
    inline fun decimalPlaces(value: Double) = value.toString().split('.').getOrElse(1) { "0" }.length

    @JvmStatic
    inline fun decimalPlaces(value: Float) = value.toString().split('.').getOrElse(1) { "0" }.length

    @JvmStatic
    inline fun isNumberEven(i: Int): Boolean {
        return i and 1 == 0
    }

    @JvmStatic
    inline fun reverseNumber(num: Int, min: Int, max: Int): Int {
        return max + min - num
    }

    @JvmStatic
    inline fun convertRange(valueIn: Int, minIn: Int, maxIn: Int, minOut: Int, maxOut: Int): Int {
        return convertRange(valueIn.toDouble(), minIn.toDouble(), maxIn.toDouble(), minOut.toDouble(), maxOut.toDouble()).toInt()
    }

    @JvmStatic
    inline fun convertRange(valueIn: Float, minIn: Float, maxIn: Float, minOut: Float, maxOut: Float): Float {
        return convertRange(valueIn.toDouble(), minIn.toDouble(), maxIn.toDouble(), minOut.toDouble(), maxOut.toDouble()).toFloat()
    }

    @JvmStatic
    inline fun convertRange(valueIn: Double, minIn: Double, maxIn: Double, minOut: Double, maxOut: Double): Double {
        val rangeIn = maxIn - minIn
        val rangeOut = maxOut - minOut
        val convertedIn = (valueIn - minIn) * (rangeOut / rangeIn) + minOut
        val actualMin = min(minOut, maxOut)
        val actualMax = max(minOut, maxOut)
        return min(max(convertedIn, actualMin), actualMax)
    }

    @JvmStatic
    inline fun lerp(from: Double, to: Double, delta: Double): Double {
        return from + (to - from) * delta
    }

    @JvmStatic
    inline fun lerp(from: Float, to: Float, delta: Float): Float {
        return from + (to - from) * delta
    }
}