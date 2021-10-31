package cum.xiaro.trollhack.module.modules.misc

import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import net.minecraft.entity.passive.AbstractChestHorse
import net.minecraft.network.play.client.CPacketUseEntity

internal object MountBypass : Module(
    name = "MountBypass",
    category = Category.MISC,
    description = "Might allow you to mount chested animals on servers that block it"
) {
    init {
        listener<PacketEvent.Send> {
            if (it.packet !is CPacketUseEntity || it.packet.action != CPacketUseEntity.Action.INTERACT_AT) return@listener
            if (it.packet.getEntityFromWorld(mc.world) is AbstractChestHorse) it.cancel()
        }
    }
}