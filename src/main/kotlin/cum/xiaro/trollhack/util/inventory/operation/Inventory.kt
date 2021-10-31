@file:Suppress("NOTHING_TO_INLINE") // Looks like inlining stuff here reduced the size of compiled code
package cum.xiaro.trollhack.util.inventory.operation

import cum.xiaro.trollhack.util.inventory.InventoryTask
import net.minecraft.inventory.Slot

/**
 * Move the item in [slotFrom]  to [slotTo] in player inventory,
 * if [slotTo] contains an item, then move it to [slotFrom]
 */
inline fun InventoryTask.Builder.moveTo(slotFrom: Slot, slotTo: Slot) {
    pickUp(slotFrom)
    pickUp(slotTo)
    pickUp(slotFrom)
}

/**
 * Move the item in [slotFrom] to [slotTo] in [windowID],
 * if [slotTo] contains an item, then move it to [slotFrom]
 */
inline fun InventoryTask.Builder.moveTo(windowID: Int, slotFrom: Slot, slotTo: Slot) {
    pickUp(windowID, slotFrom)
    pickUp(windowID, slotTo)
    pickUp(windowID, slotFrom)
}

/**
 * Move all the item that equals to the item in [slotTo] to [slotTo] in player inventory
 */
inline fun InventoryTask.Builder.moveAllTo(slotTo: Slot) {
    pickUp(slotTo)
    pickUpAll(slotTo)
    pickUp(slotTo)
}

/**
 * Move all the item that equals to the item in [slotTo] to [slotTo] in [windowID]
 */
inline fun InventoryTask.Builder.moveAllTo(windowID: Int, slotTo: Slot) {
    pickUp(windowID, slotTo)
    pickUpAll(windowID, slotTo)
    pickUp(windowID, slotTo)
}