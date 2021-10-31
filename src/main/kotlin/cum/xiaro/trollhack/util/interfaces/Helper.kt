package cum.xiaro.trollhack.util.interfaces

import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.PlayerControllerMP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.network.NetHandlerPlayClient

interface Helper {
    val mc: Minecraft
        get() = Minecraft.getMinecraft()

    val world: WorldClient?
        get() = mc.world

    val player: EntityPlayerSP?
        get() = mc.player

    val playerController: PlayerControllerMP?
        get() = mc.playerController

    val connection: NetHandlerPlayClient?
        get() = mc.connection
}