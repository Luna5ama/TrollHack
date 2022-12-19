package me.luna.trollhack.module.modules.movement

import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.event.events.PacketEvent
import me.luna.trollhack.event.events.TickEvent
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.BaritoneUtils
import me.luna.trollhack.util.MovementUtils
import me.luna.trollhack.util.threads.runSafeOrFalse
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketUseEntity

/**
 * @see me.luna.trollhack.mixins.client.player.MixinEntityPlayerSP.setSprinting
 */
internal object Sprint : Module(
    name = "Sprint",
    description = "Automatically makes the player sprint",
    category = Category.MOVEMENT
) {
    private val multiDirection by setting("Multi Direction", true, description = "Sprint in any direction")
    private val checkFlying by setting("Check Flying", true, description = "Cancels while flying")
    private val checkCollide by setting("Check Collide", true, description = "Cancels on colliding with blocks")
    private val checkCriticals by setting("Check Criticals", false, description = "Cancels on attack for criticals")

    init {
        safeListener<PacketEvent.Send>(-69420) {
            if (!it.cancelled && checkCriticals(it)) {
                connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SPRINTING))
            }
        }

        safeListener<PacketEvent.PostSend> {
            if (checkCriticals(it)) {
                connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SPRINTING))
            }
        }
    }

    private fun SafeClientEvent.checkCriticals(event: PacketEvent) =
        event.packet is CPacketUseEntity
            && checkCriticals
            && player.isSprinting
            && event.packet.action == CPacketUseEntity.Action.ATTACK
            && event.packet.getEntityFromWorld(world) is EntityLivingBase

    init {
        safeListener<TickEvent.Pre> {
            player.isSprinting = shouldSprint()
        }
    }

    @JvmStatic
    fun shouldSprint(): Boolean {
        return runSafeOrFalse {
            isEnabled
                && !mc.gameSettings.keyBindSneak.isKeyDown
                && !player.isElytraFlying
                && player.foodStats.foodLevel > 6
                && !BaritoneUtils.isPathing
                && checkMovementInput()
                && (!checkFlying || !player.capabilities.isFlying)
                && (!checkCollide || !player.collidedHorizontally)
        }
    }

    private fun SafeClientEvent.checkMovementInput() =
        if (multiDirection) MovementUtils.isInputting else
            player.movementInput.moveForward > 0.0f
}