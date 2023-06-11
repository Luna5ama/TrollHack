package dev.luna5ama.trollhack.util.inventory.operation

import dev.luna5ama.trollhack.util.inventory.InventoryTask
import net.minecraft.inventory.Slot

/**
 * Move the item in [slotFrom]  to [slotTo] in player inventory,
 * if [slotTo] contains an item, then move it to [slotFrom]
 */
fun InventoryTask.Builder.moveTo(slotFrom: Slot, slotTo: Slot) {
    pickUp(slotFrom)
    pickUp(slotTo)
    pickUp(slotFrom)
}

/**
 * Move the item in [slotFrom] to [slotTo] in [windowID],
 * if [slotTo] contains an item, then move it to [slotFrom]
 */
fun InventoryTask.Builder.moveTo(windowID: Int, slotFrom: Slot, slotTo: Slot) {
    pickUp(windowID, slotFrom)
    pickUp(windowID, slotTo)
    pickUp(windowID, slotFrom)
}

/**
 * Move all the item that equals to the item in [slotTo] to [slotTo] in player inventory
 */
fun InventoryTask.Builder.moveAllTo(slotTo: Slot) {
    pickUp(slotTo)
    pickUpAll(slotTo)
    pickUp(slotTo)
}

/**
 * Move all the item that equals to the item in [slotTo] to [slotTo] in [windowID]
 */
fun InventoryTask.Builder.moveAllTo(windowID: Int, slotTo: Slot) {
    pickUp(windowID, slotTo)
    pickUpAll(windowID, slotTo)
    pickUp(windowID, slotTo)
}