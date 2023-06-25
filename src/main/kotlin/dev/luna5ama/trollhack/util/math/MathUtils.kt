package dev.luna5ama.trollhack.util.math

import kotlin.math.*

object MathUtils {
    @JvmStatic
    fun ceilToPOT(valueIn: Int): Int {
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
    fun round(value: Float, places: Int): Float {
        val scale = 10.0f.pow(places)
        return round(value * scale) / scale
    }

    @JvmStatic
    fun round(value: Double, places: Int): Double {
        val scale = 10.0.pow(places)
        return round(value * scale) / scale
    }

    @JvmStatic
    fun decimalPlaces(value: Double) = value.toString().split('.').getOrElse(1) { "0" }.length

    @JvmStatic
    fun decimalPlaces(value: Float) = value.toString().split('.').getOrElse(1) { "0" }.length

    @JvmStatic
    fun isNumberEven(i: Int): Boolean {
        return i and 1 == 0
    }

    @JvmStatic
    fun reverseNumber(num: Int, min: Int, max: Int): Int {
        return max + min - num
    }

    @JvmStatic
    fun convertRange(valueIn: Int, minIn: Int, maxIn: Int, minOut: Int, maxOut: Int): Int {
        return convertRange(
            valueIn.toDouble(),
            minIn.toDouble(),
            maxIn.toDouble(),
            minOut.toDouble(),
            maxOut.toDouble()
        ).toInt()
    }

    @JvmStatic
    fun convertRange(valueIn: Float, minIn: Float, maxIn: Float, minOut: Float, maxOut: Float): Float {
        return convertRange(
            valueIn.toDouble(),
            minIn.toDouble(),
            maxIn.toDouble(),
            minOut.toDouble(),
            maxOut.toDouble()
        ).toFloat()
    }

    @JvmStatic
    fun convertRange(valueIn: Double, minIn: Double, maxIn: Double, minOut: Double, maxOut: Double): Double {
        val rangeIn = maxIn - minIn
        val rangeOut = maxOut - minOut
        val convertedIn = (valueIn - minIn) * (rangeOut / rangeIn) + minOut
        val actualMin = min(minOut, maxOut)
        val actualMax = max(minOut, maxOut)
        return min(max(convertedIn, actualMin), actualMax)
    }

    @JvmStatic
    fun lerp(from: Double, to: Double, delta: Double): Double {
        return from + (to - from) * delta
    }

    @JvmStatic
    fun lerp(from: Float, to: Float, delta: Float): Float {
        return from + (to - from) * delta
    }

    @JvmStatic
    fun approxEq(a: Double, b: Double, epsilon: Double = 0.0001): Boolean {
        return abs(a - b) < epsilon
    }

    @JvmStatic
    fun approxEq(a: Float, b: Float, epsilon: Float = 0.0001f): Boolean {
        return abs(a - b) < epsilon
    }

    @JvmStatic
    fun frac(value: Double): Double {
        return value - floor(value)
    }

    @JvmStatic
    fun frac(value: Float): Float {
        return value - floor(value)
    }
}