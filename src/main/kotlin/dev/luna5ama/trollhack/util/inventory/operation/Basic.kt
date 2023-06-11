package dev.luna5ama.trollhack.util.inventory.operation

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.util.inventory.*
import dev.luna5ama.trollhack.util.inventory.slot.HotbarSlot
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Slot

fun InventoryTask.Builder.pickUp(slot: SafeClientEvent.() -> Slot?) {
    pickUp(0, slot)
}

fun InventoryTask.Builder.pickUp(windowID: Int, slot: SafeClientEvent.() -> Slot?) {
    +Click(windowID, slot, 0, ClickType.PICKUP)
}

fun InventoryTask.Builder.pickUp(slot: Slot) {
    pickUp(0, slot)
}

fun InventoryTask.Builder.pickUp(windowID: Int, slot: Slot) {
    +Click(windowID, slot, 0, ClickType.PICKUP)
}

fun InventoryTask.Builder.pickUpAll(slot: SafeClientEvent.() -> Slot?) {
    pickUpAll(0, slot)
}

fun InventoryTask.Builder.pickUpAll(windowID: Int, slot: SafeClientEvent.() -> Slot?) {
    +Click(windowID, slot, 0, ClickType.PICKUP_ALL)
}

fun InventoryTask.Builder.pickUpAll(slot: Slot) {
    pickUpAll(0, slot)
}

fun InventoryTask.Builder.pickUpAll(windowID: Int, slot: Slot) {
    +Click(windowID, slot, 0, ClickType.PICKUP_ALL)
}

fun InventoryTask.Builder.quickMove(slot: SafeClientEvent.() -> Slot?) {
    quickMove(0, slot)
}

fun InventoryTask.Builder.quickMove(windowID: Int, slot: SafeClientEvent.() -> Slot?) {
    +Click(windowID, slot, 0, ClickType.QUICK_MOVE)
}

fun InventoryTask.Builder.quickMove(slot: Slot) {
    quickMove(0, slot)
}

fun InventoryTask.Builder.quickMove(windowID: Int, slot: Slot) {
    +Click(windowID, slot, 0, ClickType.QUICK_MOVE)
}

fun InventoryTask.Builder.swapWith(
    slot: SafeClientEvent.() -> Slot?,
    hotbarSlot: SafeClientEvent.() -> HotbarSlot?
) {
    swapWith(0, slot, hotbarSlot)
}

fun InventoryTask.Builder.swapWith(
    windowID: Int,
    slot: SafeClientEvent.() -> Slot?,
    hotbarSlot: SafeClientEvent.() -> HotbarSlot?
) {
    +Click(windowID, slot, { hotbarSlot.invoke(this)?.hotbarSlot }, ClickType.SWAP)
}

fun InventoryTask.Builder.swapWith(slot: Slot, hotbarSlot: HotbarSlot) {
    swapWith(0, slot, hotbarSlot)
}

fun InventoryTask.Builder.swapWith(windowID: Int, slot: Slot, hotbarSlot: HotbarSlot) {
    +Click(windowID, slot, hotbarSlot.hotbarSlot, ClickType.SWAP)
}

fun InventoryTask.Builder.throwOne(slot: Slot) {
    throwOne(0, slot)
}

fun InventoryTask.Builder.throwOne(windowID: Int, slot: Slot) {
    +Click(windowID, slot, 0, ClickType.THROW)
}

fun InventoryTask.Builder.throwAll(slot: Slot) {
    throwAll(0, slot)
}

fun InventoryTask.Builder.throwAll(windowID: Int, slot: Slot) {
    +Click(windowID, slot, 1, ClickType.THROW)
}

fun InventoryTask.Builder.action(block: SafeClientEvent.() -> Unit) {
    +object : Step {
        override fun run(event: SafeClientEvent): StepFuture {
            block.invoke(event)
            return InstantFuture
        }
    }
}