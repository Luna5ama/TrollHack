package dev.luna5ama.trollhack.utils.inventory

import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot

inline val Player.everySlots: List<Slot>
    get() = inventoryMenu.getSlots(1..45)

inline val Player.armorSlots: List<Slot>
    get() = inventoryMenu.getSlots(5..8)

inline val Player.headSlot: Slot
    get() = inventoryMenu.slots[5]

inline val Player.chestSlot: Slot
    get() = inventoryMenu.slots[6]

inline val Player.legsSlot: Slot
    get() = inventoryMenu.slots[7]

inline val Player.feetSlot: Slot
    get() = inventoryMenu.slots[8]

inline val Player.offhandSlot: Slot
    get() = inventoryMenu.slots[45]

inline val Player.craftingSlots: List<Slot>
    get() = inventoryMenu.getSlots(1..4)

inline val Player.slots: List<Slot>
    get() = inventoryMenu.getSlots(9..44)

inline val Player.storageSlots: List<Slot>
    get() = inventoryMenu.getSlots(9..35)

inline val Player.hotbarSlots: List<HotbarSlot>
    get() = ArrayList<HotbarSlot>().apply {
        for (slot in 36..44) {
            add(HotbarSlot(inventoryMenu.slots[slot]))
        }
    }

inline val Player.currentHotbarSlot: HotbarSlot
    get() = HotbarSlot(inventoryMenu.getSlot(inventory.selectedSlot + 36))

inline val Player.firstHotbarSlot: HotbarSlot
    get() = HotbarSlot(inventoryMenu.getSlot(36))

inline fun Player.getHotbarSlot(slot: Int): HotbarSlot {
    if (slot !in 0..8) throw IllegalArgumentException("Invalid hotbar slot: $slot")
    return HotbarSlot(inventoryMenu.slots[slot + 36])
}

inline fun AbstractContainerMenu.getSlots(range: IntRange): List<Slot> =
    slots.subList(range.first, range.last + 1)