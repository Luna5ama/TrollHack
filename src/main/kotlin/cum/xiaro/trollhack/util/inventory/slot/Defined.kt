@file:Suppress("NOTHING_TO_INLINE")

package cum.xiaro.trollhack.util.inventory.slot

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot

inline val EntityPlayer.allSlots: List<Slot>
    get() = inventoryContainer.getSlots(1..45)

inline val EntityPlayer.armorSlots: List<Slot>
    get() = inventoryContainer.getSlots(5..8)

inline val EntityPlayer.headSlot: Slot
    get() = inventoryContainer.inventorySlots[5]

inline val EntityPlayer.chestSlot: Slot
    get() = inventoryContainer.inventorySlots[6]

inline val EntityPlayer.legsSlot: Slot
    get() = inventoryContainer.inventorySlots[7]

inline val EntityPlayer.feetSlot: Slot
    get() = inventoryContainer.inventorySlots[8]

inline val EntityPlayer.offhandSlot: Slot
    get() = inventoryContainer.inventorySlots[45]

inline val EntityPlayer.craftingSlots: List<Slot>
    get() = inventoryContainer.getSlots(1..4)

inline val EntityPlayer.inventorySlots: List<Slot>
    get() = inventoryContainer.getSlots(9..44)

inline val EntityPlayer.storageSlots: List<Slot>
    get() = inventoryContainer.getSlots(9..35)

inline val EntityPlayer.hotbarSlots: List<HotbarSlot>
    get() = ArrayList<HotbarSlot>().apply {
        for (slot in 36..44) {
            add(HotbarSlot(inventoryContainer.inventorySlots[slot]))
        }
    }

inline val EntityPlayer.currentHotbarSlot: HotbarSlot
    get() = HotbarSlot(inventoryContainer.getSlot(inventory.currentItem + 36))

inline val EntityPlayer.firstHotbarSlot: HotbarSlot
    get() = HotbarSlot(inventoryContainer.getSlot(36))

inline fun EntityPlayer.getHotbarSlot(slot: Int): HotbarSlot {
    if (slot !in 0..8) throw IllegalArgumentException("Invalid hotbar slot: $slot")
    return HotbarSlot(inventoryContainer.inventorySlots[slot + 36])
}

inline fun Container.getSlots(range: IntRange): List<Slot> =
    inventorySlots.subList(range.first, range.last + 1)

