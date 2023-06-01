package dev.luna5ama.trollhack.module.modules.misc

import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.util.SoundCategory

internal object NoSoundLag : Module(
    name = "No Sound Lag",
    category = Category.MISC,
    description = "Prevents lag caused by sound machines"
) {
    init {
        listener<PacketEvent.Receive> {
            if (it.packet !is SPacketSoundEffect) return@listener
            if (it.packet.category == SoundCategory.PLAYERS
                && (it.packet.sound === SoundEvents.ITEM_ARMOR_EQUIP_GENERIC
                    || it.packet.sound === SoundEvents.ITEM_SHIELD_BLOCK)
            ) {
                it.cancel()
            }
        }
    }
}