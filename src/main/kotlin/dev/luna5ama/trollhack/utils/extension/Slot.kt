package dev.luna5ama.trollhack.utils.extension

import dev.luna5ama.trollhack.utils.inventory.HotbarSlot
import dev.luna5ama.trollhack.utils.inventory.SlotRanges
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.Slot

val Slot.isHotbarSlot: Boolean
    get() = container is Inventory && getContainerSlot() in SlotRanges.HOTBAR

val Slot.hotbarIndex get() = if (isHotbarSlot) getContainerSlot() else -1

fun Slot.toHotbarSlotOrNull() =
    if (isHotbarSlot) HotbarSlot(this)
    else null
