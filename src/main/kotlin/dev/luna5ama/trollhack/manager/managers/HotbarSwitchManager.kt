package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.player.HotbarUpdateEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.module.modules.client.Bypass
import dev.luna5ama.trollhack.util.inventory.slot.HotbarSlot
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.inventory.ClickType
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketClickWindow
import net.minecraft.network.play.client.CPacketHeldItemChange

object HotbarSwitchManager : Manager() {
    var serverSideHotbar = 0; private set
    var swapTime = 0L; private set

    val EntityPlayerSP.serverSideItem: ItemStack
        get() = inventory.mainInventory[serverSideHotbar]

    init {
        safeListener<PacketEvent.Send>(Int.MIN_VALUE) {
            if (it.cancelled || it.packet !is CPacketHeldItemChange) return@safeListener

            val prev = serverSideHotbar

            synchronized(HotbarSwitchManager) {
                serverSideHotbar = it.packet.slotId
                swapTime = System.currentTimeMillis()
            }

            if (prev != serverSideHotbar) {
                HotbarUpdateEvent(prev, serverSideHotbar).post()
            }
        }
    }

    fun SafeClientEvent.ghostSwitch(slot: HotbarSlot, block: () -> Unit) {
        ghostSwitch(Override.DEFAULT, slot, block)
    }

    fun SafeClientEvent.ghostSwitch(slot: Int, block: () -> Unit) {
        ghostSwitch(Override.DEFAULT, slot, block)
    }

    fun SafeClientEvent.ghostSwitch(override: Override, slot: HotbarSlot, block: () -> Unit) {
        ghostSwitch(override, slot.hotbarSlot, block)
    }

    fun SafeClientEvent.ghostSwitch(override: Override, slot: Int, block: () -> Unit) {
        synchronized(HotbarSwitchManager) {
            synchronized(InventoryTaskManager) {
                if (slot != serverSideHotbar) {
                    override.mode.run {
                        switch(slot, block)
                    }
                } else {
                    block.invoke()
                }
            }
        }
    }

    enum class BypassMode {
        NONE {
            override fun SafeClientEvent.switch(targetSlot: Int, block: () -> Unit) {
                val prevSlot = serverSideHotbar
                connection.sendPacket(CPacketHeldItemChange(targetSlot))
                block.invoke()
                connection.sendPacket(CPacketHeldItemChange(prevSlot))
            }
        },
        MOVE {
            override fun SafeClientEvent.switch(targetSlot: Int, block: () -> Unit) {
                val hotbarSlots = player.hotbarSlots
                val inventory = player.inventory
                val inventoryContainer = player.inventoryContainer
                val heldItem = player.serverSideItem
                val targetItem = inventory.mainInventory[targetSlot]

                connection.sendPacket(
                    CPacketClickWindow(
                        0,
                        hotbarSlots[targetSlot].slotNumber,
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
                        hotbarSlots[targetSlot].slotNumber,
                        0,
                        ClickType.PICKUP,
                        ItemStack.EMPTY,
                        inventoryContainer.getNextTransactionID(inventory)
                    )
                )
            }
        },
        SWAP {
            override fun SafeClientEvent.switch(targetSlot: Int, block: () -> Unit) {
                val targetSlotNumber = player.hotbarSlots[targetSlot].slotNumber
                connection.sendPacket(
                    CPacketClickWindow(
                        0,
                        targetSlotNumber,
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
                        targetSlotNumber,
                        serverSideHotbar,
                        ClickType.SWAP,
                        ItemStack.EMPTY,
                        player.inventoryContainer.getNextTransactionID(player.inventory)
                    )
                )
            }
        },
        PICK {
            override fun SafeClientEvent.switch(targetSlot: Int, block: () -> Unit) {
                playerController.pickItem(targetSlot)
                block.invoke()
                playerController.pickItem(targetSlot)
            }
        };

        abstract fun SafeClientEvent.switch(targetSlot: Int, block: () -> Unit)
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