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

package dev.luna5ama.trollhack.utils.combat

import dev.luna5ama.trollhack.manager.managers.EntityMovementManager
import dev.luna5ama.trollhack.utils.ImplicitOverriding
import dev.luna5ama.trollhack.utils.MinecraftWrapper
import dev.luna5ama.trollhack.utils.extension.actuallyDead
import dev.luna5ama.trollhack.utils.math.vectors.VectorUtils.minus
import dev.luna5ama.trollhack.utils.timing.TickTimer
import dev.luna5ama.trollhack.utils.world.EntityUtils
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3


class MotionTracker(val entity: Entity) {
    private val timestamp = TickTimer()
    private val motionLog = ArrayDeque<Vec3>()

    var prevMotion = Vec3(0.0, 0.0, 0.0); private set
    var motion = Vec3(0.0, 0.0, 0.0); private set
    var prevSpeed = Vec3(0.0, 0.0, 0.0); private set // block/tick
    var currentSpeed = Vec3(0.0, 0.0, 0.0); private set // same
    var prevAcc = Vec3(0.0, 0.0, 0.0); private set // b/t^2
    var currentAcc = Vec3(0.0, 0.0, 0.0); private set
    var jerk = Vec3(0.0, 0.0, 0.0); private set // b/t^3

    fun tick() {
        synchronized(this) {
            if (entity.actuallyDead) {
                motionLog.clear()
                prevMotion = Vec3.ZERO
                motion = Vec3.ZERO
                prevSpeed = Vec3.ZERO
                currentSpeed = Vec3.ZERO
                prevAcc = Vec3.ZERO
                currentAcc = Vec3.ZERO
                jerk = Vec3.ZERO
            }
            motionLog.add(calcActualMotion(entity))
            while (motionLog.size > 5) motionLog.removeFirstOrNull()
            prevMotion = motion
            motion = calcAverageMotion()
            prevSpeed = currentSpeed
            currentSpeed = motion - prevMotion
            prevAcc = currentAcc
            currentAcc = currentSpeed - prevSpeed
            jerk = currentAcc - prevAcc
        }
    }

    fun outdated(delay: Long) = timestamp.tick(delay)

    /**
     * Calculate the actual motion of given entity
     *
     * @param entity The entity for motion calculation
     * @return Actual motion vector
     */
    private fun calcActualMotion(entity: Entity): Vec3 {
        return Vec3(
            entity.x - entity.xo,
            entity.y - entity.yo,
            entity.z - entity.zo
        )
    }

    /**
     * Calculate the average motion
     *
     * @return Average motion vector
     */
    private fun calcAverageMotion(): Vec3 {
        var sumX = 0.0
        var sumY = 0.0
        var sumZ = 0.0

        for (motion in motionLog) {
            sumX += motion.x
            sumY += motion.y
            sumZ += motion.z
        }

        return Vec3(sumX, sumY, sumZ).scale(1.0 / motionLog.size)
    }

    /**
     * Calculate the predicted position of the target entity based on [calcAverageMotion]
     *
     * @param [ticksAhead] Amount of prediction ahead
     * @param [interpolation] Whether to return interpolated position or not, default value is false (no interpolation)
     * @return Predicted position of the target entity
     */
    fun calcPosAhead(ticksAhead: Int, interpolation: Boolean = false): Vec3 {
        val relativePos = calcRelativePosAhead(ticksAhead, interpolation)
        val partialTicks = if (interpolation) EntityMovementManager.partialTicks else 1.0f
        return EntityUtils.getInterpolatedPos(entity, partialTicks).add(relativePos)
    }


    /**
     * Calculate the predicted moved vector of the target entity based on [calcAverageMotion]
     *
     * @param [ticksAhead] Amount of prediction ahead
     * @param [interpolation] Whether to return interpolated position or not, default value is false (no interpolation)
     * @return Predicted moved vector of the target entity
     */
    @OptIn(ImplicitOverriding::class)
    fun calcRelativePosAhead(ticksAhead: Int, interpolation: Boolean = false): Vec3 {
        val world = MinecraftWrapper.world ?: return Vec3.ZERO
        val partialTicks = if (interpolation) EntityMovementManager.partialTicks else 1.0f

        val averageMotion = prevMotion.add(motion.subtract(prevMotion).scale(partialTicks.toDouble()))
        var movedVec = Vec3(0.0, 0.0, 0.0)

        for (ticks in 0..ticksAhead) {
            movedVec = if (canMove(world, entity.boundingBox, movedVec.add(averageMotion))) { // Attempt to move with full motion
                movedVec.add(averageMotion)
            } else if (canMove(world, entity.boundingBox, movedVec.add(averageMotion.x, 0.0, averageMotion.z))) { // Attempt to move horizontally
                movedVec.add(averageMotion.x, 0.0, averageMotion.z)
            } else if (canMove(world, entity.boundingBox, movedVec.add(0.0, averageMotion.y, 0.0))) {
                movedVec.add(0.0, averageMotion.y, 0.0)
            } else {
                break
            }
        }

        return movedVec
    }
    fun  Vec3.scale(scaleFactor: Double): Vec3 {
        return Vec3(x * scaleFactor,y * scaleFactor, z * scaleFactor)
    }
    private fun canMove(world: Level, bbox: AABB, offset: Vec3): Boolean {
        return !world.noCollision(bbox.move(offset))
    }
}