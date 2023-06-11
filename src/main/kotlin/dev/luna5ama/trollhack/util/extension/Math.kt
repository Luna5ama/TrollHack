package dev.luna5ama.trollhack.util.extension

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor

const val PI_FLOAT = 3.14159265358979323846f

const val FLOOR_DOUBLE_D = 1_073_741_824.0
const val FLOOR_DOUBLE_I = 1_073_741_824

const val FLOOR_FLOAT_F = 4_194_304.0f
const val FLOOR_FLOAT_I = 4_194_304

fun Double.floorToInt() = floor(this).toInt()
fun Float.floorToInt() = floor(this).toInt()

fun Double.ceilToInt() = ceil(this).toInt()
fun Float.ceilToInt() = ceil(this).toInt()

fun Double.fastFloor() = (this + FLOOR_DOUBLE_D).toInt() - FLOOR_DOUBLE_I
fun Float.fastFloor() = (this + FLOOR_FLOAT_F).toInt() - FLOOR_FLOAT_I

fun Double.fastCeil() = FLOOR_DOUBLE_I - (FLOOR_DOUBLE_D - this).toInt()
fun Float.fastCeil() = FLOOR_FLOAT_I - (FLOOR_FLOAT_F - this).toInt()

fun Float.toRadian() = this / 180.0f * PI_FLOAT
fun Double.toRadian() = this / 180.0 * PI

fun Float.toDegree() = this * 180.0f / PI_FLOAT
fun Double.toDegree() = this * 180.0 / PI

val Double.sq: Double get() = this * this
val Float.sq: Float get() = this * this
val Int.sq: Int get() = this * this

val Double.cubic: Double get() = this * this * this
val Float.cubic: Float get() = this * this * this
val Int.cubic: Int get() = this * this * this

val Double.quart: Double get() = this * this * this * this
val Float.quart: Float get() = this * this * this * this
val Int.quart: Int get() = this * this * this * this

val Double.quint: Double get() = this * this * this * this * this
val Float.quint: Float get() = this * this * this * this * this
val Int.quint: Int get() = this * this * this * this * this