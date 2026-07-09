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
@file:Suppress("nothing_to_inline", "unused")

package dev.luna5ama.trollhack.utils.math

import net.minecraft.world.phys.AABB
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor

const val PI_FLOAT: Float = 3.1415926535897932384626f

const val FLOOR_DOUBLE_D: Double = 1_073_741_824.0
const val FLOOR_DOUBLE_I: Int = 1_073_741_824

const val FLOOR_FLOAT_F: Float = 4_194_304.0f
const val FLOOR_FLOAT_I: Int = 4_194_304

inline fun Double.floorToInt(): Int = floor(this).toInt()
inline fun Float.floorToInt(): Int = floor(this).toInt()

inline fun Double.ceilToInt(): Int = ceil(this).toInt()
inline fun Float.ceilToInt(): Int = ceil(this).toInt()

inline fun Double.fastFloor(): Int = (this + FLOOR_DOUBLE_D).toInt() - FLOOR_DOUBLE_I
inline fun Double.fastFloorToDouble(): Double = this.fastFloor().toDouble()
inline fun Float.fastFloor(): Int = (this + FLOOR_FLOAT_F).toInt() - FLOOR_FLOAT_I
inline fun Float.fastFloorToFloat(): Float = this.fastFloor().toFloat()

inline fun Double.fastCeil(): Int = FLOOR_DOUBLE_I - (FLOOR_DOUBLE_D - this).toInt()
inline fun Float.fastCeil(): Int = FLOOR_FLOAT_I - (FLOOR_FLOAT_F - this).toInt()

inline fun Float.toRadian(): Float = this / 180.0f * PI_FLOAT
inline fun Double.toRadian(): Double = this / 180.0 * PI

inline fun AABB.scale(size: Float): AABB {
    val centerX = this.minX + (this.maxX - this.minX) / 2
    val centerY = this.minY + (this.maxY - this.minY) / 2
    val centerZ = this.minZ + (this.maxZ - this.minZ) / 2

    return AABB(
        centerX + size / 100,
        centerY + size / 100,
        centerZ + size / 100,
        centerX - size / 100,
        centerY - size / 100,
        centerZ - size / 100
    )
}

inline fun Float.toDegree(): Float = this * 180.0f / PI_FLOAT
inline fun Double.toDegree(): Double = this * 180.0 / PI

inline val Double.sq: Double get() = this * this
inline val Float.sq: Float get() = this * this
inline val Int.sq: Int get() = this * this

inline val Double.cubic: Double get() = this * this * this
inline val Float.cubic: Float get() = this * this * this
inline val Int.cubic: Int get() = this * this * this

inline val Double.quart: Double get() = this * this * this * this
inline val Float.quart: Float get() = this * this * this * this
inline val Int.quart: Int get() = this * this * this * this

inline val Double.quint: Double get() = this * this * this * this * this
inline val Float.quint: Float get() = this * this * this * this * this
inline val Int.quint: Int get() = this * this * this * this * this


val Int.isEven: Boolean get() = this and 1 == 0
val Int.isOdd: Boolean get() = this and 1 == 1