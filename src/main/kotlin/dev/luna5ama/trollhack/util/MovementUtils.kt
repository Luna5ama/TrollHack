package dev.luna5ama.trollhack.util

import dev.fastmc.common.toDegree
import dev.fastmc.common.toRadians
import dev.luna5ama.trollhack.util.math.RotationUtils
import dev.luna5ama.trollhack.util.math.vector.Vec3f
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.MobEffects
import net.minecraft.util.MovementInput
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

object MovementUtils {
    private val mc = Minecraft.getMinecraft()

    fun isInputting(
        movementInput: MovementInput? = Wrapper.player?.movementInput,
        jump: Boolean = false,
        sneak: Boolean = false
    ): Boolean {
        if (movementInput == null) return false
        return movementInput.moveForward != 0.0f
            || movementInput.moveStrafe != 0.0f
            || jump && movementInput.jump
            || sneak && movementInput.sneak
    }

    val Entity.isMoving get() = speed > 0.0001
    val Entity.speed get() = hypot(motionX, motionZ)
    val Entity.realSpeed get() = hypot(posX - prevPosX, posZ - prevPosZ)

    val Entity.realMotionX get() = posX - prevPosX
    val Entity.realMotionY get() = posY - prevPosY
    val Entity.realMotionZ get() = posZ - prevPosZ

    /* totally not taken from elytrafly */
    fun EntityPlayerSP.calcMoveYaw(): Double {
        return calcMoveYaw(
            yaw = rotationYaw,
            moveForward = movementInput.moveForward,
            moveStrafe = movementInput.moveStrafe
        )
    }

    fun calcMoveYaw(
        yaw: Float,
        moveForward: Float,
        moveStrafe: Float
    ): Double {
        val moveYaw = if (moveForward == 0.0f && moveStrafe == 0.0f) 0.0
        else atan2(moveForward, moveStrafe).toDegree() - 90.0
        return RotationUtils.normalizeAngle(yaw + moveYaw).toRadians()
    }

    fun calcMovementInput(
        forward: Boolean,
        backward: Boolean,
        left: Boolean,
        right: Boolean,
        up: Boolean,
        down: Boolean,
    ): Vec3f {
        var moveForward = 0.0f
        var moveStrafing = 0.0f
        var moveVertical = 0.0f

        if (forward) moveForward++
        if (backward) moveForward--

        if (left) moveStrafing++
        if (right) moveStrafing--

        if (up) moveVertical++
        if (down) moveVertical--

        return Vec3f(moveStrafing, moveVertical, moveForward)
    }

    fun EntityLivingBase.applySpeedPotionEffects(speed: Double): Double {
        var result = speed

        this.getActivePotionEffect(MobEffects.SPEED)?.let {
            result += speed * (it.amplifier + 1.0) * 0.2
        }

        this.getActivePotionEffect(MobEffects.SLOWNESS)?.let {
            result -= speed * (it.amplifier + 1.0) * 0.15
        }

        return result
    }

    val EntityLivingBase.speedEffectMultiplier: Double
        get() {
            var result = 1.0

            this.getActivePotionEffect(MobEffects.SPEED)?.let {
                result += (it.amplifier + 1.0) * 0.2
            }

            this.getActivePotionEffect(MobEffects.SLOWNESS)?.let {
                result -= (it.amplifier + 1.0) * 0.15
            }

            return result
        }

    fun EntityLivingBase.applyJumpBoostPotionEffects(motion: Double): Double {
        return this.getActivePotionEffect(MobEffects.JUMP_BOOST)?.let {
            motion + (it.amplifier + 1.0) * 0.2
        } ?: motion
    }

    fun EntityPlayerSP.isCentered(center: BlockPos): Boolean {
        return this.isCentered(center.x + 0.5, center.z + 0.5)
    }

    fun EntityPlayerSP.isCentered(center: Vec3d): Boolean {
        return this.isCentered(center.x, center.z)
    }

    fun EntityPlayerSP.isCentered(x: Double, z: Double): Boolean {
        return abs(this.posX - x) < 0.2
            && abs(this.posZ - z) < 0.2
    }

    fun MovementInput.resetMove() {
        moveForward = 0.0f
        moveStrafe = 0.0f
        forwardKeyDown = false
        backKeyDown = false
        leftKeyDown = false
        rightKeyDown = false
    }

    fun MovementInput.resetJumpSneak() {
        jump = false
        sneak = false
    }
}