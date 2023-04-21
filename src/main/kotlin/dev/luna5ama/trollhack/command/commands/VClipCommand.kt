package dev.luna5ama.trollhack.command.commands

import dev.luna5ama.trollhack.command.ClientCommand
import net.minecraft.network.play.client.CPacketPlayer

object VClipCommand : ClientCommand(
    name = "vclip",
    description = "Attempts to clip vertically."
) {
    init {
        double("offset") { offsetArg ->
            executeSafe {
                val posX = player.posX
                val posY = player.posY + offsetArg.value
                val posZ = player.posZ
                val onGround = player.onGround

                player.setPosition(posX, posY, posZ)
                connection.sendPacket(CPacketPlayer.Position(posX, posY, posZ, onGround))
            }
        }
    }
}