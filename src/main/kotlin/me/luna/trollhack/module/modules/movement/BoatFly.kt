package me.luna.trollhack.module.modules.movement

import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.event.events.InputEvent
import me.luna.trollhack.event.events.PacketEvent
import me.luna.trollhack.event.events.player.PlayerTravelEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.module.modules.misc.AntiAFK
import me.luna.trollhack.module.modules.player.Freecam
import me.luna.trollhack.util.MovementUtils
import me.luna.trollhack.util.MovementUtils.calcMoveYaw
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityBoat
import net.minecraft.network.play.client.CPacketInput
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketVehicleMove
import net.minecraft.network.play.server.SPacketMoveVehicle
import net.minecraft.util.EnumHand
import net.minecraft.world.chunk.EmptyChunk
import org.lwjgl.input.Keyboard
import kotlin.math.cos
import kotlin.math.sin

internal object BoatFly : Module(
    name = "BoatFly",
    description = "Fly using boats (Bypass 2b2t.xin)",
    category = Category.MOVEMENT,
    modulePriority = 1000
) {
    private val speed by setting("Speed", 2.5f, 0.1f..25.0f, 0.1f)
    private val antiStuck by setting("Anti Stuck", true)
    private val glideSpeed by setting("Glide Speed", 0f, 0.0f..1.0f, 0.01f)
    private val upSpeed by setting("Up Speed", 3.5f, 0.0f..5.0f, 0.1f)
    private val downSpeed by setting("Down Speed", 3.5f, 0.0f..5.0f, 0.1f, description = "key: LCtrl")
    val opacity by setting("Boat Opacity", 1.0f, 0.0f..1.0f, 0.01f)
    val size by setting("Boat Scale", 0.5, 0.05..1.5, 0.01)
    private val forceInteract by setting("Force Interact", false)
    private val interactTickDelay by setting("Interact Delay", 2, 1..20, 1, { forceInteract }, description = "Force interact packet delay, in ticks.")

    init {
        safeListener<PacketEvent.Send> {
            val ridingEntity = player.ridingEntity

            if (!forceInteract || ridingEntity !is EntityBoat) return@safeListener

            if (it.packet is CPacketPlayer.Rotation || it.packet is CPacketInput) {
                it.cancel()
            }

            if (it.packet is CPacketVehicleMove) {
                if (player.ticksExisted % interactTickDelay == 0) {
                    playerController.interactWithEntity(player, ridingEntity, EnumHand.MAIN_HAND)
                }
            }
        }

        safeListener<PacketEvent.Receive> {
            if (!forceInteract || player.ridingEntity !is EntityBoat || it.packet !is SPacketMoveVehicle) return@safeListener
            it.cancel()
        }

        safeListener<PlayerTravelEvent> {
            player.ridingEntity?.let { entity ->
                if (entity is EntityBoat && entity.controllingPassenger == player) {
                    steerEntity(entity, speed, antiStuck)

                    // Make sure the boat doesn't turn etc (params: isLeftDown, isRightDown, isForwardDown, isBackDown)
                    entity.rotationYaw = player.rotationYaw
                    entity.updateInputs(false, false, false, false)

                    fly(entity)
                }
            }
        }
    }

    private fun fly(entity: Entity) {
        if (!entity.isInWater) entity.motionY = -glideSpeed.toDouble()
        if(!Freecam.isEnabled){
            if (mc.gameSettings.keyBindJump.isKeyDown) entity.motionY += upSpeed / 2.0
            if (!entity.isInWater && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) entity.motionY -= downSpeed / 2.0
        }
    }

    @JvmStatic
    fun isBoatFlying(entityIn: Entity): Boolean {
        return isEnabled && mc.player?.ridingEntity == entityIn
    }

    @JvmStatic
    fun shouldModifyScale(entityIn: Entity): Boolean {
        return isBoatFlying(entityIn) && mc.gameSettings.thirdPersonView == 0
    }

    private fun SafeClientEvent.steerEntity(entity: Entity, speed: Float, antiStuck: Boolean) {
        val yawRad = calcMoveYaw()

        val motionX = -sin(yawRad) * speed
        val motionZ = cos(yawRad) * speed

        if (MovementUtils.isInputting && !isBorderingChunk(entity, motionX, motionZ, antiStuck)) {
            entity.motionX = motionX
            entity.motionZ = motionZ
        } else {
            entity.motionX = 0.0
            entity.motionZ = 0.0
        }
    }

    private fun SafeClientEvent.isBorderingChunk(entity: Entity, motionX: Double, motionZ: Double, antiStuck: Boolean): Boolean {
        return antiStuck && world.getChunk((entity.posX + motionX).toInt() shr 4, (entity.posZ + motionZ).toInt() shr 4) is EmptyChunk
    }
}
