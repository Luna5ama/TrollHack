package dev.luna5ama.trollhack.utils.math.vectors

import dev.luna5ama.trollhack.utils.math.toRadian
import net.minecraft.core.Vec3i
import kotlin.math.*

data class Vec2i(val x: Int = 0, val y: Int = 0) {
    constructor(x: Float, y: Float) : this(x.toInt(), y.toInt())
    constructor(x: Double, y: Double) : this(x.toInt(), y.toInt())

    constructor(vec3i: Vec3i) : this(vec3i.x, vec3i.y)

    constructor(vec2d: Vec2i) : this(vec2d.x, vec2d.y)

    fun toRadians() = Vec2d(x.toDouble().toRadian(), y.toDouble().toRadian())

    fun toPolar() = when {
        (x > 0 && y > 0) || (x > 0 && y < 0) -> Vec2d(length(), asin(y / length()))
        (x < 0 && y > 0) -> Vec2d(length(), acos(x / length()))
        (x < 0 && y < 0) -> Vec2d(length(), PI - asin(y / length()))
        else -> Vec2d.ZERO
    }

    fun length() = hypot(x.toDouble(), y.toDouble())

    fun lengthSquared() = x * x + y * y

    operator fun div(vec2d: Vec2i) = div(vec2d.x, vec2d.y)

    operator fun div(divider: Int) = div(divider, divider)

    fun div(x: Int, y: Int) = Vec2i(this.x / x, this.y / y)

    operator fun times(multiplier: Int) = scale(multiplier, multiplier)

    fun dot(vec2i: Vec2i) = this.x * vec2i.x + this.y * vec2i.y

    fun cross(vec2i: Vec2i): Nothing = throw UnsupportedOperationException("2-dimension vectors don't have out products.")

    fun scale(vec2d: Vec2i) = scale(vec2d.x, vec2d.y)

    fun scale(x: Int, y: Int) = Vec2i(this.x * x, this.y * y)

    operator fun minus(vec2d: Vec2i) = minus(vec2d.x, vec2d.y)

    operator fun minus(vec2d: Vec2f) = Vec2f(x, y) - vec2d

    operator fun minus(sub: Int) = minus(sub, sub)

    fun minus(x: Int, y: Int) = plus(-x, -y)

    operator fun plus(vec2d: Vec2i) = plus(vec2d.x, vec2d.y)

    operator fun plus(add: Int) = plus(add, add)

    fun plus(x: Int, y: Int) = Vec2i(this.x + x, this.y + y)

    override fun toString(): String {
        return "Vec2i[${this.x}, ${this.y}]"
    }

    companion object {
        @JvmField
        val ZERO = Vec2i(0, 0)
    }
}