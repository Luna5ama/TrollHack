package cum.xiaro.trollhack.module.modules.misc

import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.util.SoundCategory

internal object NoSoundLag : Module(
    name = "NoSoundLag",
    category = Category.MISC,
    description = "Prevents lag caused by sound machines"
) {
    init {
        listener<PacketEvent.Receive> {
            if (it.packet !is SPacketSoundEffect) return@listener
            if (it.packet.category == SoundCategory.PLAYERS
                && (it.packet.sound === SoundEvents.ITEM_ARMOR_EQUIP_GENERIC
                    || it.packet.sound === SoundEvents.ITEM_SHIELD_BLOCK)) {
                it.cancel()
            }
        }
    }
}