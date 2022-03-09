package me.luna.trollhack.module.modules.player

import me.luna.trollhack.event.events.PacketEvent
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.accessor.windowID
import me.luna.trollhack.util.inventory.slot.craftingSlots
import me.luna.trollhack.util.inventory.slot.hasAnyItem
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