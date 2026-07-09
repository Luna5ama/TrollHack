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

package dev.luna5ama.trollhack.utils.math

import net.minecraft.world.entity.LivingEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import kotlin.math.*
import kotlin.system.measureNanoTime

@Suppress("NOTHING_TO_INLINE")
internal object MathUtils {
    fun absMinus(d1: Double, d2: Double): Double {
        return abs(abs(d1) - abs(d2))
    }

    inline fun square(input: Double): Double {
        return input * input
    }

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
    inline fun calcSegments(segmentsIn: Int, radius: Float, range: Float): Int {
        if (segmentsIn != -0) return segmentsIn
        val segments = radius * 0.5 * PI * (range / 360.0)
        if (segments.isNaN()) return 16
        return max(segments.roundToInt(), 16)
    }

    @JvmStatic
    inline fun convertRange(valueIn: Int, minIn: Int, maxIn: Int, minOut: Int, maxOut: Int): Int {
        return convertRange(
            valueIn.toDouble(),
            minIn.toDouble(),
            maxIn.toDouble(),
            minOut.toDouble(),
            maxOut.toDouble()
        ).toInt()
    }

    @JvmStatic
    inline fun convertRange(valueIn: Float, minIn: Float, maxIn: Float, minOut: Float, maxOut: Float): Float {
        return convertRange(
            valueIn.toDouble(),
            minIn.toDouble(),
            maxIn.toDouble(),
            minOut.toDouble(),
            maxOut.toDouble()
        ).toFloat()
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

    fun getDirectionFromEntityLiving(pos: BlockPos, entity: LivingEntity): Direction {
        if (abs(entity.x - (pos.x.toDouble() + 0.5)) < 2.0 && abs(entity.z - (pos.z.toDouble() + 0.5)) < 2.0) {
            val d0 = entity.y + entity.getEyeHeight(entity.pose).toDouble()
            if (d0 - pos.y.toDouble() > 2.0) {
                return Direction.UP
            }

            if (pos.y.toDouble() - d0 > 0.0) {
                return Direction.DOWN
            }
        }

        return entity.direction.opposite
    }

    fun getFacingOrder(yaw: Float, pitch: Float): Direction {
        val f = pitch * 0.017453292f
        val g = -yaw * 0.017453292f
        val h = Mth.sin(f.toDouble())
        val i = Mth.cos(f.toDouble())
        val j = Mth.sin(g.toDouble())
        val k = Mth.cos(g.toDouble())
        val bl = j > 0.0f
        val bl2 = h < 0.0f
        val bl3 = k > 0.0f
        val l = if (bl) j else -j
        val m = if (bl2) -h else h
        val n = if (bl3) k else -k
        val o = l * i
        val p = n * i
        val direction = if (bl) Direction.EAST else Direction.WEST
        val direction2 = if (bl2) Direction.UP else Direction.DOWN
        val direction3 = if (bl3) Direction.SOUTH else Direction.NORTH
        return if (l > n) {
            if (m > o) {
                direction2
            } else {
                direction
            }
        } else if (m > p) {
            direction2
        } else {
            direction3
        }
    }
}
