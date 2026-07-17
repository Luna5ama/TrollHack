package dev.luna5ama.trollhack.utils.inventory

import net.minecraft.world.inventory.Slot

class HotbarSlot(slot: Slot) : Slot(slot.container, slot.getContainerSlot(), slot.x, slot.y) {
    init {
        index = slot.index
    }

    val hotbarSlot = slot.getContainerSlot()
}
