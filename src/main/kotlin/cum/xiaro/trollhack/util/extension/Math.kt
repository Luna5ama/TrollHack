@file:Suppress("NOTHING_TO_INLINE")

package cum.xiaro.trollhack.util.extension

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor

const val PI_FLOAT = 3.14159265358979323846f

const val FLOOR_DOUBLE_D = 1_073_741_824.0
const val FLOOR_DOUBLE_I = 1_073_741_824

const val FLOOR_FLOAT_F = 4_194_304.0f
const val FLOOR_FLOAT_I = 4_194_304

inline fun Double.floorToInt() = floor(this).toInt()
inline fun Float.floorToInt() = floor(this).toInt()

inline fun Double.ceilToInt() = ceil(this).toInt()
inline fun Float.ceilToInt() = ceil(this).toInt()

inline fun Double.fastFloor() = (this + FLOOR_DOUBLE_D).toInt() - FLOOR_DOUBLE_I
inline fun Float.fastFloor() = (this + FLOOR_FLOAT_F).toInt() - FLOOR_FLOAT_I

inline fun Double.fastCeil() = FLOOR_DOUBLE_I - (FLOOR_DOUBLE_D - this).toInt()
inline fun Float.fastCeil() = FLOOR_FLOAT_I - (FLOOR_FLOAT_F - this).toInt()

inline fun Float.toRadian() = this / 180.0f * PI_FLOAT
inline fun Double.toRadian() = this / 180.0 * PI

inline fun Float.toDegree() = this * 180.0f / PI_FLOAT
inline fun Double.toDegree() = this * 180.0 / PI

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