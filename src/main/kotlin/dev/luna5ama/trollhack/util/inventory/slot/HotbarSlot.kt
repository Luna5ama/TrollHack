package dev.luna5ama.trollhack.util.inventory.slot

import net.minecraft.inventory.Slot

class HotbarSlot(slot: Slot) : Slot(slot.inventory, slot.slotIndex, slot.xPos, slot.yPos) {
    init {
        slotNumber = slot.slotNumber
    }

    val hotbarSlot = slot.slotNumber - 36

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HotbarSlot) return false

        return hotbarSlot == other.hotbarSlot
    }

    override fun hashCode(): Int {
        return hotbarSlot
    }
}