package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.player.HotbarUpdateEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.util.inventory.slot.HotbarSlot
import dev.luna5ama.trollhack.util.inventory.slot.hotbarIndex
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import dev.luna5ama.trollhack.util.inventory.slot.isHotbarSlot
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketClickWindow
import net.minecraft.network.play.client.CPacketHeldItemChange

object HotbarSwitchManager : Manager() {
    var serverSideHotbar = 0; private set
    var swapTime = 0L; private set

    private var serverSizeItemOverride: ItemStack? = null

    val EntityPlayerSP.serverSideItem: ItemStack
        get() = serverSizeItemOverride ?: inventory.mainInventory[serverSideHotbar]

    init {
        safeListener<PacketEvent.Send>(Int.MIN_VALUE) {
            if (it.cancelled || it.packet !is CPacketHeldItemChange) return@safeListener

            val prev: Int

            synchronized(InventoryTaskManager) {
                prev = serverSideHotbar
                serverSideHotbar = it.packet.slotId
                swapTime = System.currentTimeMillis()
            }

            if (prev != it.packet.slotId) {
                HotbarUpdateEvent(prev, serverSideHotbar).post()
            }
        }
    }

    fun SafeClientEvent.ghostSwitch(slot: Slot, block: () -> Unit) {
        ghostSwitch(Override.DEFAULT, slot, block)
    }

    fun SafeClientEvent.ghostSwitch(slot: Int, block: () -> Unit) {
        ghostSwitch(Override.DEFAULT, slot, block)
    }

    fun SafeClientEvent.ghostSwitch(override: Override, slot: Slot, block: () -> Unit) {
        synchronized(InventoryTaskManager) {
            serverSizeItemOverride = slot.stack
            if (slot.hotbarIndex != serverSideHotbar) {
                override.mode.run {
                    switch(slot, block)
                }
                return
            }
            serverSizeItemOverride = null
        }

        block.invoke()
    }

    fun SafeClientEvent.ghostSwitch(override: Override, slot: Int, block: () -> Unit) {
        ghostSwitch(override, player.hotbarSlots[slot], block)
    }

    enum class BypassMode {
        NONE {
            override fun SafeClientEvent.switch(targetSlot: Slot, block: () -> Unit) {
                if (!targetSlot.isHotbarSlot) {
                    SWAP.run {
                        switch(targetSlot, block)
                        return
                    }
                }

                val prevSlot = serverSideHotbar
                connection.sendPacket(CPacketHeldItemChange(targetSlot.hotbarIndex))
                block.invoke()
                connection.sendPacket(CPacketHeldItemChange(prevSlot))
            }
        },
        MOVE {
            override fun SafeClientEvent.switch(targetSlot: Slot, block: () -> Unit) {
                val hotbarSlots = player.hotbarSlots
                val inventory = player.inventory
                val inventoryContainer = player.inventoryContainer
                val heldItem = player.serverSideItem
                val targetItem = targetSlot.stack

                connection.sendPacket(
                    CPacketClickWindow(
                        0,
                        targetSlot.slotNumber,
                        0,
                        ClickType.PICKUP,
                        targetItem,
                        inventoryContainer.getNextTransactionID(inventory)
                    )
                )
                connection.sendPacket(
                    CPacketClickWindow(
                        0,
                        hotbarSlots[serverSideHotbar].slotNumber,
                        0,
                        ClickType.PICKUP,
                        heldItem,
                        inventoryContainer.getNextTransactionID(inventory)
                    )
                )

                block.invoke()

                connection.sendPacket(
                    CPacketClickWindow(
                        0,
                        hotbarSlots[serverSideHotbar].slotNumber,
                        0,
                        ClickType.PICKUP,
                        targetItem,
                        inventoryContainer.getNextTransactionID(inventory)
                    )
                )
                connection.sendPacket(
                    CPacketClickWindow(
                        0,
                        targetSlot.slotNumber,
                        0,
                        ClickType.PICKUP,
                        ItemStack.EMPTY,
                        inventoryContainer.getNextTransactionID(inventory)
                    )
                )
            }
        },
        SWAP {
            override fun SafeClientEvent.switch(targetSlot: Slot, block: () -> Unit) {
                connection.sendPacket(
                    CPacketClickWindow(
                        0,
                        targetSlot.slotNumber,
                        serverSideHotbar,
                        ClickType.SWAP,
                        ItemStack.EMPTY,
                        player.inventoryContainer.getNextTransactionID(player.inventory)
                    )
                )
                block.invoke()
                connection.sendPacket(
                    CPacketClickWindow(
                        0,
                        targetSlot.slotNumber,
                        serverSideHotbar,
                        ClickType.SWAP,
                        ItemStack.EMPTY,
                        player.inventoryContainer.getNextTransactionID(player.inventory)
                    )
                )
            }
        },
        PICK {
            override fun SafeClientEvent.switch(targetSlot: Slot, block: () -> Unit) {
                if (targetSlot.slotNumber == 45 || targetSlot.slotNumber < 9) {
                    SWAP.run {
                        switch(targetSlot, block)
                        return
                    }
                }
                val number = targetSlot.slotNumber % 36
                playerController.pickItem(number)
                block.invoke()
                playerController.pickItem(number)
            }
        };

        abstract fun SafeClientEvent.switch(targetSlot: Slot, block: () -> Unit)
    }

    enum class Override {
        DEFAULT {
            override val mode get() = Bypass.ghostSwitchBypass
        },
        NONE {
            override val mode = BypassMode.NONE
        },
        MOVE {
            override val mode = BypassMode.MOVE
        },
        SWAP {
            override val mode = BypassMode.SWAP
        },
        PICK {
            override val mode = BypassMode.PICK
        };

        abstract val mode: BypassMode
    }
}