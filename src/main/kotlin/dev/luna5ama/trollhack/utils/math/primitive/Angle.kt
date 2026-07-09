package dev.luna5ama.trollhack.utils.math.primitive

import dev.luna5ama.trollhack.utils.math.toRadian
import kotlin.math.PI

private const val ONE_RADIUS = PI / 180.0

data class Radius(private val radius: Double) : Number() {
    fun toAngle() = Angle(radius / ONE_RADIUS)

    override fun toByte(): Byte {
        TODO("Not yet implemented")
    }

    @Deprecated(
        "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.\nIf you override toChar() function in your Number inheritor, it's recommended to gradually deprecate the overriding function and then remove it.\nSee https://youtrack.jetbrains.com/issue/KT-46465 for details about the migration",
        replaceWith = ReplaceWith("this.toInt().toChar()")
    )
    override fun toChar(): Char {
        TODO("Not yet implemented")
    }

    override fun toDouble(): Double {
        return radius
    }

    override fun toFloat(): Float {
        return radius.toFloat()
    }

    override fun toInt(): Int {
        TODO("Not yet implemented")
    }

    override fun toLong(): Long {
        TODO("Not yet implemented")
    }

    override fun toShort(): Short {
        TODO("Not yet implemented")
    }

    operator fun plus(other: Radius) = Radius(this.toDouble().plus(other.toDouble()))
    operator fun minus(other: Radius) = Radius(this.toDouble().minus(other.toDouble()))
    operator fun times(other: Radius) = Radius(this.toDouble().times(other.toDouble()))
    operator fun div(other: Radius) = Radius(this.toDouble().div(other.toDouble()))

    operator fun plus(other: Double) = Radius(this.toDouble().plus(other))
    operator fun minus(other: Double) = Radius(this.toDouble().minus(other))
    operator fun times(other: Double) = Radius(this.toDouble().times(other))
    operator fun div(other: Double) = Radius(this.toDouble().div(other))
}

data class Angle(private val angle: Double) : Number() {
    fun toRadius() = Radius(angle.toRadian())

    override fun toByte(): Byte {
        TODO("Not yet implemented")
    }

    @Deprecated(
        "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.\nIf you override toChar() function in your Number inheritor, it's recommended to gradually deprecate the overriding function and then remove it.\nSee https://youtrack.jetbrains.com/issue/KT-46465 for details about the migration",
        replaceWith = ReplaceWith("this.toInt().toChar()")
    )
    override fun toChar(): Char {
        TODO("Not yet implemented")
    }

    override fun toDouble(): Double {
        return angle
    }

    override fun toFloat(): Float {
        return angle.toFloat()
    }

    override fun toInt(): Int {
        TODO("Not yet implemented")
    }

    override fun toLong(): Long {
        TODO("Not yet implemented")
    }

    override fun toShort(): Short {
        TODO("Not yet implemented")
    }

    operator fun plus(other: Angle) = Angle(this.toDouble().plus(other.toDouble()))
    operator fun minus(other: Angle) = Angle(this.toDouble().minus(other.toDouble()))
    operator fun times(other: Angle) = Angle(this.toDouble().times(other.toDouble()))
    operator fun div(other: Angle) = Angle(this.toDouble().div(other.toDouble()))

    operator fun plus(other: Double) = Angle(this.toDouble().plus(other))
    operator fun minus(other: Double) = Angle(this.toDouble().minus(other))
    operator fun times(other: Double) = Angle(this.toDouble().times(other))
    operator fun div(other: Double) = Angle(this.toDouble().div(other))
}