/*
 * Copyright (c) 2021-2022, SagiriXiguajerry. All rights reserved.
 * This repository will be transformed to SuperMic_233.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package dev.luna5ama.trollhack.graphics.color

import dev.luna5ama.trollhack.utils.math.MathUtils
import kotlin.math.max
import kotlin.math.roundToInt

class ColorGradient(vararg stops: Stop) {
    private val colorArray = stops.apply { sortBy { it.value } }

    fun get(valueIn: Float): ColorRGBA {
        if (colorArray.isEmpty()) return ColorRGBA(255, 255, 255)
        var prevStop = colorArray.last()
        var nextStop = colorArray.last()
        for ((index, stop) in colorArray.withIndex()) {
            if (stop.value < valueIn) continue
            prevStop = if (stop.value == valueIn) stop else colorArray[max(index - 1, 0)]
            nextStop = stop
            break
        }
        if (prevStop == nextStop) return prevStop.color
        val r = MathUtils.convertRange(
            valueIn,
            prevStop.value,
            nextStop.value,
            prevStop.color.r.toFloat(),
            nextStop.color.r.toFloat()
        ).roundToInt()
        val g = MathUtils.convertRange(
            valueIn,
            prevStop.value,
            nextStop.value,
            prevStop.color.g.toFloat(),
            nextStop.color.g.toFloat()
        ).roundToInt()
        val b = MathUtils.convertRange(
            valueIn,
            prevStop.value,
            nextStop.value,
            prevStop.color.b.toFloat(),
            nextStop.color.b.toFloat()
        ).roundToInt()
        val a = MathUtils.convertRange(
            valueIn,
            prevStop.value,
            nextStop.value,
            prevStop.color.a.toFloat(),
            nextStop.color.a.toFloat()
        ).roundToInt()
        return ColorRGBA(r, g, b, a)
    }

    class Stop(val value: Float, val color: ColorRGBA)
}