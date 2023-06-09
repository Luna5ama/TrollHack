package dev.luna5ama.trollhack.util.inventory.slot

import dev.luna5ama.trollhack.util.Wrapper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import java.util.function.Predicate

/**
 * Find an empty slot or slot that matches [predicate]
 * or slot 0 if none of those were found
 */
fun EntityPlayer.anyHotbarSlot(predicate: Predicate<ItemStack>? = null): HotbarSlot {
    val hotbarSlots = this.hotbarSlots
    return hotbarSlots.firstEmpty()
        ?: hotbarSlots.firstByStack(predicate)
        ?: this.firstHotbarSlot
}

/**
 * Find an empty slot or slot 0
 */
fun EntityPlayer.anyHotbarSlot() =
    this.hotbarSlots.firstEmpty()
        ?: this.firstHotbarSlot


val Slot.isHotbarSlot: Boolean
    get() = this.slotNumber in 36..44 && this.inventory == Wrapper.player?.inventory

val Slot.hotbarIndex get() = if (isHotbarSlot) this.slotNumber - 36 else -1

fun Slot.toHotbarSlotOrNull() =
    if (isHotbarSlot) HotbarSlot(this)
    else null