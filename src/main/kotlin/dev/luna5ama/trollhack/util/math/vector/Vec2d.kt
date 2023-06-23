package dev.luna5ama.trollhack.util.math.vector

import dev.fastmc.common.*
import net.minecraft.util.math.Vec3d

data class Vec2d(val x: Double = 0.0, val y: Double = 0.0) {
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())

    constructor(vec3d: Vec3d) : this(vec3d.x, vec3d.y)

    constructor(vec2d: Vec2d) : this(vec2d.x, vec2d.y)

    fun toRadians() = Vec2d(x.toRadians(), y.toRadians())

    fun distanceTo(vec2d: Vec2d) = distance(x, y, vec2d.x, vec2d.y)

    fun distanceSqTo(vec2d: Vec2d) = distanceSq(x, y, vec2d.x, vec2d.y)

    fun length() = length(x, y)

    fun lengthSq() = lengthSq(x, y)


    operator fun div(vec2d: Vec2d) = div(vec2d.x, vec2d.y)

    operator fun div(divider: Double) = div(divider, divider)

    fun div(x: Double, y: Double) = Vec2d(this.x / x, this.y / y)


    operator fun times(vec2d: Vec2d) = times(vec2d.x, vec2d.y)

    operator fun times(multiplier: Double) = times(multiplier, multiplier)

    fun times(x: Double, y: Double) = Vec2d(this.x * x, this.y * y)


    operator fun minus(vec2d: Vec2d) = minus(vec2d.x, vec2d.y)

    operator fun minus(sub: Double) = minus(sub, sub)

    fun minus(x: Double, y: Double) = plus(-x, -y)


    operator fun plus(vec2d: Vec2d) = plus(vec2d.x, vec2d.y)

    operator fun plus(add: Double) = plus(add, add)

    fun plus(x: Double, y: Double) = Vec2d(this.x + x, this.y + y)

    fun toVec2f() = Vec2f(x.toFloat(), y.toFloat())

    override fun toString(): String {
        return "Vec2d[${this.x}, ${this.y}]"
    }

    companion object {
        @JvmField
        val ZERO = Vec2d(0.0, 0.0)
    }
}