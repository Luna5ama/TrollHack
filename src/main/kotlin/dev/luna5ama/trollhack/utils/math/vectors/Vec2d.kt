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

package dev.luna5ama.trollhack.utils.math.vectors

import dev.luna5ama.trollhack.utils.math.primitive.Radius
import dev.luna5ama.trollhack.utils.math.toRadian
import net.minecraft.world.phys.Vec3
import org.joml.Matrix2d
import org.joml.Vector2d
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

data class Vec2d(val x: Double = 0.0, val y: Double = 0.0) {
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())

    constructor(Vec3: Vec3) : this(Vec3.x, Vec3.y)

    constructor(vec2d: Vec2d) : this(vec2d.x, vec2d.y)

    constructor(vec2d: Vector2d) : this(vec2d.x, vec2d.y)

    fun toRadians() = Vec2d(x.toRadian(), y.toRadian())

    fun length() = hypot(x, y)

    fun lengthSquared() = (this.x.pow(2) + this.y.pow(2))

    fun rotate(rad: Radius) = Vec2d(Vector2d(x, y).mul(Matrix2d(cos(rad.toDouble()), sin(rad.toDouble()), -sin(rad.toDouble()), cos(rad.toDouble()))))

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

    fun toVec2f() = Vec2f(x, y)

    override fun toString(): String {
        return "Vec2d[${this.x}, ${this.y}]"
    }

    companion object {
        @JvmField
        val ZERO = Vec2d(0.0, 0.0)
    }
}
