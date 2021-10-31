package cum.xiaro.trollhack.manager.managers

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.Manager
import cum.xiaro.trollhack.util.accessor.currentPlayerItem
import cum.xiaro.trollhack.util.inventory.slot.HotbarSlot
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

    inline fun SafeClientEvent.spoofHotbar(slot: HotbarSlot, block: () -> Unit) {
        //contract {
        //    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        //}

        synchronized(playerController) {
            spoofHotbar(slot)
            block.invoke()
            resetHotbar()
        }
    }

    inline fun SafeClientEvent.spoofHotbar(slot: Int, block: () -> Unit) {
        //contract {
        //    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        //}

        synchronized(playerController) {
            spoofHotbar(slot)
            block.invoke()
            resetHotbar()
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