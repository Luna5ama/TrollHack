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

package dev.luna5ama.trollhack.utils.extension



import dev.luna5ama.trollhack.manager.managers.EntityMovementManager
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.compat.forwardImpulseCompat
import dev.luna5ama.trollhack.utils.compat.leftImpulseCompat
import dev.luna5ama.trollhack.utils.math.floorToInt
import dev.luna5ama.trollhack.utils.math.vectors.VectorUtils.plus
import dev.luna5ama.trollhack.utils.math.vectors.VectorUtils.times
import dev.luna5ama.trollhack.utils.world.EntityUtils
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

val Entity.offset: Vec3
    get() = Vec3(x - xo, y - yo, z - zo)

val Entity.offsetX: Double
    get() = this.offset.x

val Entity.offsetY: Double
    get() = this.offset.y

val Entity.offsetZ: Double
    get() = this.offset.z

val Entity.flooredPosition get() = BlockPos(this.x.floorToInt(), this.y.floorToInt(), this.z.floorToInt())
val Entity.betterPosition
    get() = BlockPos(
        this.x.floorToInt(),
        (this.y + 0.25).floorToInt(),
        this.z.floorToInt()
    )

val Entity.realSpeed get() = hypot(x - xo, z - zo)

val LivingEntity.scaledHealth: Float
    get() = this.health + this.absorptionAmount * (this.health / this.maxHealth)

val LivingEntity.totalHealth: Float
    get() = this.health + this.absorptionAmount

val LivingEntity.realHealth get() = this.health + this.absorptionAmount

fun Entity.predicted(predictFactor: Int): Vec3 {
    return EntityMovementManager.getPredictedVec3Map(predictFactor, true)[this]
        ?: (this.position() + this.offset * predictFactor.toDouble())
}

context(ctx: NonNullContext)
fun Entity.predictHorizontally(ticks: Int, smooth: Boolean = false): Vec3 {
    val entity = this
    val motionX = (entity.x - entity.xo).coerceIn(-0.6, 0.6)
    val motionZ = (entity.z - entity.zo).coerceIn(-0.6, 0.6)

    val entityBox = entity.boundingBox
    var targetBox = entityBox

    for (tick in 0..<ticks) {
        targetBox = ctx.canMove(targetBox, motionX, 0.0, motionZ, entity)
                    ?: ctx.canMove(targetBox, 0.0, 0.0, 0.0, entity)
                    ?: break
    }

    val offsetX = targetBox.minX - entityBox.minX
    val offsetZ = targetBox.minZ - entityBox.minZ
    val motion = Vec3(offsetX, 0.0, offsetZ)
    return if (smooth) {
        EntityUtils.getInterpolatedPos(entity, EntityMovementManager.partialTicks).add(motion)
    } else position().add(motion)
}

fun Player.isMoving(): Boolean {
    return this.velocityX != 0.0 || this.velocityY != 0.0|| this.velocityZ !=0.0
}

fun Player.getDistance2D(): Double {
    val xDist: Double = this.x - this.xo
    val zDist: Double = this.z - this.zo
    return sqrt(xDist * xDist + zDist * zDist)
}


fun LocalPlayer.getMoveForward(): Float {
    return this.input.forwardImpulseCompat
}

fun LocalPlayer.getMoveStrafe(): Float {
    return this.input.leftImpulseCompat
}

fun LocalPlayer.forward(d: Double): DoubleArray {
    var f: Float = this.input.forwardImpulseCompat
    var f2: Float = this.input.leftImpulseCompat
    var f3: Float = this.yRot
    if (f != 0.0f) {
        if (f2 > 0.0f) {
            f3 += (if ((f > 0.0f)) -45 else 45).toFloat()
        } else if (f2 < 0.0f) {
            f3 += (if ((f > 0.0f)) 45 else -45).toFloat()
        }
        f2 = 0.0f
        if (f > 0.0f) {
            f = 1.0f
        } else if (f < 0.0f) {
            f = -1.0f
        }
    }
    val d2 = sin(Math.toRadians((f3 + 90.0f).toDouble()))
    val d3 = cos(Math.toRadians((f3 + 90.0f).toDouble()))
    val d4 = f * d * d3 + f2 * d * d2
    val d5 = f * d * d2 - f2 * d * d3
    return doubleArrayOf(d4, d5)
}

var Player.velocityX: Double
    get() = deltaMovement.x
    set(value) {
        setDeltaMovement(
            value,
            velocityY,
            velocityZ
        )
    }

var Player.velocityY: Double
    get() = deltaMovement.y
    set(value) {
        setDeltaMovement(
            velocityX,
            value,
            velocityZ
        )
    }

var Player.velocityZ: Double
    get() = deltaMovement.z
    set(value) {
        setDeltaMovement(
            velocityX,
            velocityY,
            value
        )
    }

var Entity.prevYaw
    get() = yRotO
    set(value) {
        yRotO = value
    }

var Entity.prevPitch
    get() = xRotO
    set(value) {
        xRotO = value
    }

var Entity.yaw
    get() = yRot
    set(value) {
        yRot = value
    }

var Entity.pitch
    get() = xRot
    set(value) {
        xRot = value
    }

context(ctx: NonNullContext)
fun Entity.getSpeedKpH(): Double {
    val distTraveledLastTickX: Double = this.x - this.xo
    val distTraveledLastTickZ: Double = this.z - this.zo
    val speedometerCurrentSpeed =
        distTraveledLastTickX * distTraveledLastTickX + distTraveledLastTickZ * distTraveledLastTickZ

    var speedOMeterKPHD = sqrt(speedometerCurrentSpeed.toFloat()) * 71.2729367892
    speedOMeterKPHD = (10.0 * speedOMeterKPHD).roundToInt().toDouble() / 10.0
    return speedOMeterKPHD
}

context(ctx: NonNullContext)
fun Entity.collisionPredict(ticks: Int, smooth: Boolean = false): Vec3 {
    val entity = this
    val motionX = (entity.x - entity.xo).coerceIn(-0.6, 0.6)
    val motionY = (entity.y - entity.yo).coerceIn(-0.5, 0.5)
    val motionZ = (entity.z - entity.zo).coerceIn(-0.6, 0.6)

    val entityBox = entity.boundingBox
    var targetBox = entityBox


    for (tick in 0..ticks) {
        targetBox = ctx.canMove(targetBox, motionX, motionY, motionZ, entity)
            ?: ctx.canMove(targetBox, motionX, 0.0, motionZ, entity)
                    ?: ctx.canMove(targetBox, 0.0, motionY, 0.0, entity)
                    ?: break

    }


    val offsetX = targetBox.minX - entityBox.minX
    val offsetY = targetBox.minY - entityBox.minY
    val offsetZ = targetBox.minZ - entityBox.minZ
    val motion = Vec3(offsetX, offsetY, offsetZ)
    return if (smooth) {
        EntityUtils.getInterpolatedPos(entity, EntityMovementManager.partialTicks).add(motion)
    } else position().add(motion)
}

fun NonNullContext.canMove(box: AABB, x: Double, y: Double, z: Double, entity: Entity): AABB? {
    return box.move(x, y, z).takeIf { !world.collidesWithSuffocatingBlock(entity,it) }
}

val Entity.actuallyDead get() = isRemoved || (this is LivingEntity && this.realHealth <= 0f)
