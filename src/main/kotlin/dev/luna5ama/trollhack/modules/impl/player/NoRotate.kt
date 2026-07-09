package dev.luna5ama.trollhack.modules.impl.player

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.mixins.accessor.IPositionMoveRotationAccessor
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.extension.pitch
import dev.luna5ama.trollhack.utils.extension.yaw
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket

object NoRotate : Module("No Rotate", "No Rotate", Category.PLAYER) {
    init {
        nonNullHandler<PacketEvent.Receive> {
            if (it.packet is ClientboundPlayerPositionPacket) {
                val yaw = player.yaw
                val pitch = player.pitch
                (it.packet.change as IPositionMoveRotationAccessor).setYRot(yaw)
                (it.packet.change as IPositionMoveRotationAccessor).setXRot(pitch)
            }
        }
    }
}
