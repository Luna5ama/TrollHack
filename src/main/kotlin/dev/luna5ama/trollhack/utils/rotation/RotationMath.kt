package dev.luna5ama.trollhack.utils.rotation

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot

object RotationMath {
    @JvmStatic
    fun step(
        lastYaw: Float,
        lastPitch: Float,
        targetYaw: Float,
        targetPitch: Float,
        maxDegrees: Float,
        sensitivityStep: Float
    ): FloatArray {
        if (maxDegrees <= 0.0f) return floatArrayOf(lastYaw, lastPitch)

        val deltaYaw = wrapDegrees(targetYaw - lastYaw)
        val deltaPitch = targetPitch - lastPitch
        val distance = hypot(deltaYaw.toDouble(), deltaPitch.toDouble())
        if (distance < 1.0E-6) return floatArrayOf(lastYaw, lastPitch)

        val scale = (maxDegrees / distance).coerceAtMost(1.0).toFloat()
        val desiredYaw = deltaYaw * scale
        val desiredPitch = deltaPitch * scale
        if (sensitivityStep <= 0.0f) {
            return floatArrayOf(lastYaw + desiredYaw, lastPitch + desiredPitch)
        }

        val yawSteps = candidateSteps(desiredYaw / sensitivityStep)
        val pitchSteps = candidateSteps(desiredPitch / sensitivityStep)
        var bestYaw = 0.0f
        var bestPitch = 0.0f
        var bestError = Float.POSITIVE_INFINITY

        for (yawStep in yawSteps) {
            val yaw = yawStep * sensitivityStep
            for (pitchStep in pitchSteps) {
                val pitch = pitchStep * sensitivityStep
                val resultPitch = lastPitch + pitch
                if (resultPitch !in -90.0f..90.0f) continue
                if (hypot(yaw.toDouble(), pitch.toDouble()) > maxDegrees + 1.0E-6) continue

                val error = square(yaw - desiredYaw) + square(pitch - desiredPitch)
                if (error < bestError) {
                    bestError = error
                    bestYaw = yaw
                    bestPitch = pitch
                }
            }
        }

        return floatArrayOf(lastYaw + bestYaw, lastPitch + bestPitch)
    }

    private fun candidateSteps(value: Float): IntArray {
        val lower = floor(value).toInt()
        val upper = ceil(value).toInt()
        return buildSet {
            add(0)
            for (step in (lower - 1)..(upper + 1)) add(step)
        }.toIntArray()
    }

    private fun wrapDegrees(degrees: Float): Float {
        var wrapped = degrees % 360.0f
        if (wrapped >= 180.0f) wrapped -= 360.0f
        if (wrapped < -180.0f) wrapped += 360.0f
        return wrapped
    }

    private fun square(value: Float): Float = value * value
}
