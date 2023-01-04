package me.luna.trollhack.manager.managers

import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.event.events.PacketEvent
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.manager.Manager
import me.luna.trollhack.util.accessor.currentPlayerItem
import me.luna.trollhack.util.inventory.inventoryTaskNow
import me.luna.trollhack.util.inventory.operation.action
import me.luna.trollhack.util.inventory.operation.swapWith
import me.luna.trollhack.util.inventory.slot.HotbarSlot
import me.luna.trollhack.util.inventory.slot.hotbarSlots
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketHeldItemChange

@Suppress("NOTHING_TO_INLINE")
object HotbarManager : Manager() {
    var serverSideHotbar = 0; private set
    var swapTime = 0L; private set

    val EntityPlayerSP.serverSideItem: ItemStack
        get() = inventory.mainInventory[serverSideHotbar]

    init {
        safeListener<PacketEvent.Send>(Int.MIN_VALUE) {
            if (it.cancelled || it.packet !is CPacketHeldItemChange) return@safeListener

            synchronized(playerController) {
                serverSideHotbar = it.packet.slotId
                swapTime = System.currentTimeMillis()
            }
        }
    }

    inline fun SafeClientEvent.spoofHotbar(slot: HotbarSlot, crossinline block: () -> Unit) {
        synchronized(playerController) {
            spoofHotbar(slot)
            block.invoke()
            resetHotbar()
        }
    }

    inline fun SafeClientEvent.spoofHotbar(slot: Int, crossinline block: () -> Unit) {
        synchronized(playerController) {
            spoofHotbar(slot)
            block.invoke()
            resetHotbar()
        }
    }

    inline fun SafeClientEvent.spoofHotbarBypass(slot: HotbarSlot, crossinline block: () -> Unit) {
        synchronized(playerController) {
            val swap = slot.hotbarSlot != serverSideHotbar
            if (swap) {
                inventoryTaskNow {
                    val hotbarSlot = player.hotbarSlots[serverSideHotbar]
                    swapWith(slot, hotbarSlot)
                    action { block.invoke() }
                    swapWith(slot, hotbarSlot)
                }
            } else {
                block.invoke()
            }
        }
    }

    inline fun SafeClientEvent.spoofHotbarBypass(slot: Int, crossinline block: () -> Unit) {
        synchronized(playerController) {
            val swap = slot != serverSideHotbar
            if (swap) {
                inventoryTaskNow {
                    val slotFrom = player.hotbarSlots[serverSideHotbar]
                    val hotbarSlot = player.hotbarSlots[serverSideHotbar]
                    swapWith(slotFrom, hotbarSlot)
                    action { block.invoke() }
                    swapWith(slotFrom, hotbarSlot)
                }
            } else {
                block.invoke()
            }
        }
    }

    inline fun SafeClientEvent.spoofHotbar(slot: HotbarSlot) {
        return spoofHotbar(slot.hotbarSlot)
    }

    inline fun SafeClientEvent.spoofHotbar(slot: Int) {
        if (serverSideHotbar != slot) {
            connection.sendPacket(CPacketHeldItemChange(slot))
        }
    }

    inline fun SafeClientEvent.resetHotbar() {
        val slot = playerController.currentPlayerItem
        if (serverSideHotbar != slot) {
            spoofHotbar(slot)
        }
    }
}