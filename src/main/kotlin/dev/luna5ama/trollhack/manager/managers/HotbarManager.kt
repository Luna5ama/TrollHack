package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.player.HotbarUpdateEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.util.accessor.currentPlayerItem
import dev.luna5ama.trollhack.util.inventory.inventoryTaskNow
import dev.luna5ama.trollhack.util.inventory.operation.action
import dev.luna5ama.trollhack.util.inventory.operation.swapWith
import dev.luna5ama.trollhack.util.inventory.slot.HotbarSlot
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
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

            val prev = serverSideHotbar

            synchronized(playerController) {
                serverSideHotbar = it.packet.slotId
                swapTime = System.currentTimeMillis()
            }

            if (prev != serverSideHotbar) {
                HotbarUpdateEvent(prev, serverSideHotbar).post()
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