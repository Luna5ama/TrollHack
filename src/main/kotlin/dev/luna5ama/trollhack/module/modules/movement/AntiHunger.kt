package dev.luna5ama.trollhack.module.modules.movement

import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.accessor.onGround
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayer

/**
 * Movement taken from Seppuku
 * https://github.com/seppukudevelopment/seppuku/blob/005e2da/src/main/java/me/rigamortis/seppuku/impl/module/player/NoHungerModule.java
 */
internal object AntiHunger : Module(
    name = "Anti Hunger",
    category = Category.MOVEMENT,
    description = "Reduces hunger lost when moving around"
) {
    private val cancelMovementState = setting("Cancel Movement State", true)

    init {
        listener<PacketEvent.Send> {
            if (mc.player == null) return@listener
            if (cancelMovementState.value && it.packet is CPacketEntityAction) {
                if (it.packet.action == CPacketEntityAction.Action.START_SPRINTING || it.packet.action == CPacketEntityAction.Action.STOP_SPRINTING) {
                    it.cancel()
                }
            }
            if (it.packet is CPacketPlayer) {
                // Trick the game to think that tha player is flying even if he is on ground. Also check if the player is flying with the Elytra.
                it.packet.onGround =
                    (mc.player.fallDistance <= 0 || mc.playerController.isHittingBlock) && mc.player.isElytraFlying
            }
        }
    }
}