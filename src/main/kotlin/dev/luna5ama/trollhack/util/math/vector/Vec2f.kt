package dev.luna5ama.trollhack.util.math.vector

import dev.fastmc.common.distance
import dev.fastmc.common.distanceSq
import dev.fastmc.common.sq
import dev.fastmc.common.toRadians
import net.minecraft.entity.Entity
import kotlin.math.sqrt

@JvmInline
value class Vec2f private constructor(val bits: Long) {

    constructor(x: Float, y: Float) : this((x.toRawBits().toLong() shl 32) or (y.toRawBits().toLong() and 0xFFFFFFFF))

    /**
     * Create a Vec2f from this entity's rotations
     */
    constructor(entity: Entity) : this(entity.rotationYaw, entity.rotationPitch)

    constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())

    constructor(vec2d: Vec2d) : this(vec2d.x.toFloat(), vec2d.y.toFloat())

    val x: Float
        get() = getX(bits)

    val y: Float
        get() = getY(bits)

    fun toRadians(): Vec2f {
        return Vec2f(x.toRadians(), y.toRadians())
    }

    fun distanceSqTo(vec2f: Vec2f) = distanceSq(x, y, vec2f.x, vec2f.y)

    fun distanceTo(vec2f: Vec2f) = distance(x, y, vec2f.x, vec2f.y)

    fun length() = sqrt(lengthSq())

    fun lengthSq() = x.sq + y.sq


    operator fun div(vec2f: Vec2f) = div(vec2f.x, vec2f.y)

    operator fun div(divider: Float) = div(divider, divider)

    fun div(x: Float, y: Float) = Vec2f(this.x / x, this.y / y)


    operator fun times(vec2f: Vec2f) = times(vec2f.x, vec2f.y)

    operator fun times(multiplier: Float) = times(multiplier, multiplier)

    fun times(x: Float, y: Float) = Vec2f(this.x * x, this.y * y)


    operator fun minus(vec2f: Vec2f) = minus(vec2f.x, vec2f.y)

    operator fun minus(value: Float) = minus(value, value)

    fun minus(x: Float, y: Float) = plus(-x, -y)


    operator fun plus(vec2f: Vec2f) = plus(vec2f.x, vec2f.y)

    operator fun plus(value: Float) = plus(value, value)

    fun plus(x: Float, y: Float) = Vec2f(this.x + x, this.y + y)

    fun toVec2d() = Vec2d(x.toDouble(), y.toDouble())

    operator fun component1() = x

    operator fun component2() = y

    companion object {
        val ZERO = Vec2f(0f, 0f)

        @JvmStatic
        fun getX(bits: Long): Float {
            return Float.fromBits((bits shr 32).toInt())
        }

        @JvmStatic
        fun getY(bits: Long): Float {
            return Float.fromBits((bits and 0xFFFFFFFF).toInt())
        }
    }
}