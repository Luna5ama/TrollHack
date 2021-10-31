package cum.xiaro.trollhack.util

import cum.xiaro.trollhack.TrollHackMod
import cum.xiaro.trollhack.event.events.ShutdownEvent
import cum.xiaro.trollhack.util.ConfigUtils.saveAll
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient

object Wrapper {
    @JvmStatic
    val minecraft: Minecraft
        get() = Minecraft.getMinecraft()

    @JvmStatic
    val player: EntityPlayerSP?
        get() = minecraft.player

    @JvmStatic
    val world: WorldClient?
        get() = minecraft.world

    @JvmStatic
    fun saveAndShutdown() {
        if (!TrollHackMod.ready) return

        ShutdownEvent.post()
        saveAll()
    }
}