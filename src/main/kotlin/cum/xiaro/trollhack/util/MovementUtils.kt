package cum.xiaro.trollhack.util

import cum.xiaro.trollhack.event.SafeClientEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.MobEffects
import net.minecraft.util.MovementInput
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Suppress("NOTHING_TO_INLINE")
object MovementUtils {
    private val mc = Minecraft.getMinecraft()

    inline val isInputtingAny: Boolean
        get() = isInputting || Wrapper.player?.movementInput?.let {
            it.jump || it.sneak
        } ?: false

    inline val isInputting
        get() = Wrapper.player?.movementInput?.let {
            it.moveForward != 0.0f || it.moveStrafe != 0.0f
        } ?: false

    inline val Entity.isMoving get() = speed > 0.0001
    inline val Entity.speed get() = hypot(motionX, motionZ)
    inline val Entity.realSpeed get() = hypot(posX - prevPosX, posZ - prevPosZ)

    /* totally not taken from elytrafly */
    fun SafeClientEvent.calcMoveYaw(yawIn: Float = mc.player.rotationYaw, moveForward: Float = roundedForward, moveString: Float = roundedStrafing): Double {
        var strafe = 90 * moveString
        strafe *= if (moveForward != 0F) moveForward * 0.5F else 1F

        var yaw = yawIn - strafe
        yaw -= if (moveForward < 0F) 180 else 0

        return Math.toRadians(yaw.toDouble())
    }

    private inline val roundedForward get() = getRoundedMovementInput(mc.player.movementInput.moveForward)
    private inline val roundedStrafing get() = getRoundedMovementInput(mc.player.movementInput.moveStrafe)

    private fun getRoundedMovementInput(input: Float) = when {
        input > 0f -> 1f
        input < 0f -> -1f
        else -> 0f
    }

    fun SafeClientEvent.setSpeed(speed: Double) {
        val yaw = calcMoveYaw()
        player.motionX = -sin(yaw) * speed
        player.motionZ = cos(yaw) * speed
    }

    inline fun EntityLivingBase.applySpeedPotionEffects(speed: Double): Double {
        return this.getActivePotionEffect(MobEffects.SPEED)?.let {
            speed * this.speedEffectMultiplier
        } ?: speed
    }

    inline val EntityLivingBase.speedEffectMultiplier: Double
        get() = this.getActivePotionEffect(MobEffects.SPEED)?.let {
            1.0 + (it.amplifier + 1.0) * 0.2
        } ?: 1.0

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