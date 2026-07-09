package dev.luna5ama.trollhack.utils.math.vectors

import dev.luna5ama.trollhack.utils.math.sq
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class Vec3f(val x: Float, val y: Float, val z: Float) {
    constructor() : this(0f, 0f, 0f)

    companion object {
        @JvmField
        val ZERO = Vec3f(0.0f, 0.0f, 0.0f)
    }

    operator fun times(vec3f: Vec3f) = times(vec3f.x, vec3f.y, vec3f.z)

    operator fun times(multiplier: Float) = times(multiplier, multiplier, multiplier)

    fun times(x: Float, y: Float, z: Float) = Vec3f(this.x * x, this.y * y, this.z * z)

    fun normalized(): Vec3f {
        val divisor = sqrt(x.sq + y.sq + z.sq)
        return Vec3f(x / divisor, y / divisor, z / divisor)
    }
}

fun Vec3f(yaw: Float, pitch: Float): Vec3f {
    val x = cos(yaw)
    val z = sin(yaw)
    val y = sqrt(x * x + z * z) * tan(pitch)
    return Vec3f(x, y, z)
}