package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.accessor.windowID
import dev.luna5ama.trollhack.util.inventory.slot.craftingSlots
import dev.luna5ama.trollhack.util.inventory.slot.hasAnyItem
import net.minecraft.network.play.client.CPacketCloseWindow

internal object XCarry : Module(
    name = "XCarry",
    category = Category.PLAYER,
    description = "Store items in crafting slots"
) {
    init {
        safeListener<PacketEvent.Send> {
            if (it.packet is CPacketCloseWindow && it.packet.windowID == 0 && (!player.inventory.itemStack.isEmpty || player.craftingSlots.hasAnyItem())) {
                it.cancel()
            }
        }
    }
}