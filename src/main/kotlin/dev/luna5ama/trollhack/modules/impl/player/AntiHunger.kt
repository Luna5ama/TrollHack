package dev.luna5ama.trollhack.modules.impl.player

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.mixins.accessor.IPlayerMoveC2SPacketAccessor
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket

object AntiHunger : Module(
    "Anti Hunger",
    "Reduces (does NOT remove) hunger consumption.",
    Category.PLAYER
) {
    private val sprint by setting("Sprint",true)
    private val onGround by setting("On Ground", true)

    init {
        nonNullHandler<PacketEvent.Send> {
            if (player.isPassenger || player.isInWater || player.isUnderWater) return@nonNullHandler
            if (it.packet is ServerboundPlayerCommandPacket && sprint) {
                if (it.packet.action == ServerboundPlayerCommandPacket.Action.START_SPRINTING) it.cancel()
            }
            if (it.packet is ServerboundMovePlayerPacket && onGround
                && player.onGround() && player.fallDistance <= 0.0 && !interaction.isDestroying) {
                (it.packet as IPlayerMoveC2SPacketAccessor).setOnGround(false)
            }
        }
    }
}
