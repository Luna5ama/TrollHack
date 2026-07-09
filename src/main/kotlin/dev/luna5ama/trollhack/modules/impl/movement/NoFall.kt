package dev.luna5ama.trollhack.modules.impl.movement

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.UpdateEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.extension.velocityY
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.item.Items

object NoFall : Module(
    "No Fall",
    "Prevents fall damage.",
    Category.MOVEMENT
) {
    init {
        nonNullHandler<UpdateEvent> {
            if (!isHoldingMace(player) && isFallingFastEnoughToCauseDamage(player)) {
                netHandler.send(ServerboundMovePlayerPacket.StatusOnly(true, player.horizontalCollision))
            }
        }
    }

    private fun isHoldingMace(player: LocalPlayer): Boolean {
        return player.mainHandItem.`is`(Items.MACE)
    }

    private fun isFallingFastEnoughToCauseDamage(player: LocalPlayer): Boolean {
        return player.velocityY < -0.5
    }
}