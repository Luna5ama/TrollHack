package dev.luna5ama.trollhack.manager.managers

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap
import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.event.impl.player.HotbarUpdateEvent
import dev.luna5ama.trollhack.manager.AbstractManager
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.extension.hotbarIndex
import dev.luna5ama.trollhack.utils.extension.isHotbarSlot
import dev.luna5ama.trollhack.utils.inventory.SlotRanges
import dev.luna5ama.trollhack.utils.inventory.hotbarSlots
import net.minecraft.network.HashedStack
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

object HotbarSwitchManager : AbstractManager(), AlwaysListening {
    var serverSideHotbar = 0; private set
    var swapTime = 0L; private set

    val Player.serverSideItem: ItemStack
        get() = hotbarSlots[serverSideHotbar].item

    init {
        nonNullHandler<PacketEvent.Send>(Int.MIN_VALUE) {
            val packet = it.packet
            if (it.cancelled || packet !is ServerboundSetCarriedItemPacket) return@nonNullHandler

            val prev: Int

            synchronized(InventoryManager) {
                prev = serverSideHotbar
                serverSideHotbar = packet.slot
                swapTime = System.currentTimeMillis()
            }

            if (prev != it.packet.slot) {
                HotbarUpdateEvent(prev, serverSideHotbar).post()
            }
        }
    }

    context(ctx: NonNullContext)
    fun ghostSwitch(slot: Slot, block: () -> Unit): Unit = ctx.run {
        ghostSwitch(Override.DEFAULT, slot, block)
    }

    context(ctx: NonNullContext)
    fun ghostSwitch(slot: Int, block: () -> Unit): Unit = ctx.run {
        ghostSwitch(Override.DEFAULT, slot, block)
    }

    context(ctx: NonNullContext)
    fun ghostSwitch(override: Override, slot: Slot, block: () -> Unit): Unit = ctx.run {
        synchronized(InventoryManager) {
            if (slot.hotbarIndex != serverSideHotbar) {
                override.mode.run {
                    switch(slot, block)
                }
                return
            }
        }
        block.invoke()
    }

    context(ctx: NonNullContext)
    fun ghostSwitchc( slot: Int, block: () -> Unit): Unit = ctx.run {
      val  i =  player.inventory.selectedSlot
        doSwap(slot)
        block.invoke()
        doSwap(i)
    }
    context(ctx: NonNullContext)
    fun doSwap(slot: Int): Unit = ctx.run {
        player.inventory.selectedSlot = slot
        netHandler.send(ServerboundSetCarriedItemPacket(slot))
    }

    private fun NonNullContext.menuSlotId(slot: Slot): Int {
        val menu = player.containerMenu
        val current = menu.slots.getOrNull(slot.index)
        if (current != null && current.container === slot.container &&
            current.getContainerSlot() == slot.getContainerSlot()
        ) {
            return slot.index
        }

        if (slot.container !is Inventory) return -1
        return SlotRanges.indexToId(slot.getContainerSlot())
    }

    context(ctx: NonNullContext)
    fun ghostSwitch(override: Override, slot: Int, block: () -> Unit): Unit = ctx.run {
        val menuSlot = SlotRanges.indexToId(slot)
        if (menuSlot < 0) return@run
        ghostSwitch(override, player.containerMenu.getSlot(menuSlot), block)
    }

    enum class BypassMode : Displayable {
        NONE {
            override fun NonNullContext.switch(targetSlot: Slot, block: () -> Unit) {
                if (!targetSlot.isHotbarSlot) {
                    SWAP.run {
                        switch(targetSlot, block)
                        return
                    }
                }

                val targetId = targetSlot.hotbarIndex
                val prevSlot = serverSideHotbar
                player.inventory.selectedSlot = targetId
                netHandler.send(ServerboundSetCarriedItemPacket(targetId))
                block.invoke()
                player.inventory.selectedSlot = prevSlot
                netHandler.send(ServerboundSetCarriedItemPacket(prevSlot))
            }
        },
        MOVE {
            override fun NonNullContext.switch(targetSlot: Slot, block: () -> Unit) {
                val inventoryContainer = player.containerMenu
                val targetId = menuSlotId(targetSlot)
                val hotbarId = SlotRanges.indexToId(serverSideHotbar)
                if (targetId < 0 || hotbarId < 0) return

                netHandler.send(
                    ServerboundContainerClickPacket(
                        inventoryContainer.containerId,
                        inventoryContainer.incrementStateId(),
                        targetId.toShort(),
                        0.toByte(),
                        ContainerInput.PICKUP,
                        Int2ObjectArrayMap(mapOf(targetId to HashedStack.EMPTY)),
                        HashedStack.EMPTY
                    )
                )
                netHandler.send(
                    ServerboundContainerClickPacket(
                        inventoryContainer.containerId,
                        inventoryContainer.incrementStateId(),
                        hotbarId.toShort(),
                        0.toByte(),
                        ContainerInput.PICKUP,
                        Int2ObjectArrayMap(mapOf(hotbarId to HashedStack.EMPTY)),
                        HashedStack.EMPTY
                    )
                )

                block.invoke()

                netHandler.send(
                    ServerboundContainerClickPacket(
                        inventoryContainer.containerId,
                        inventoryContainer.incrementStateId(),
                        hotbarId.toShort(),
                        0.toByte(),
                        ContainerInput.PICKUP,
                        Int2ObjectArrayMap(mapOf(hotbarId to HashedStack.EMPTY)),
                        HashedStack.EMPTY
                    )
                )
                netHandler.send(
                    ServerboundContainerClickPacket(
                        inventoryContainer.containerId,
                        inventoryContainer.incrementStateId(),
                        targetId.toShort(),
                        0.toByte(),
                        ContainerInput.PICKUP,
                        Int2ObjectArrayMap(mapOf(targetId to HashedStack.EMPTY)),
                        HashedStack.EMPTY
                    )
                )
            }
        },
        SWAP {
            override fun NonNullContext.switch(targetSlot: Slot, block: () -> Unit) {
                val targetId = menuSlotId(targetSlot)
                if (targetId < 0) return
                if (ClientSettings.inventorySwapBypass) {
                    interaction.handleContainerInput(player.containerMenu.containerId, targetId, 0,
                        ContainerInput.PICKUP, player)
                } else {
                    interaction.handleContainerInput(player.containerMenu.containerId,
                        targetId, serverSideHotbar, ContainerInput.SWAP, player)
                }
                block.invoke()
                if (ClientSettings.inventorySwapBypass) {
                    interaction.handleContainerInput(player.containerMenu.containerId, targetId, 0, ContainerInput.PICKUP, player)
                } else {
                    interaction.handleContainerInput(player.containerMenu.containerId,
                        targetId, serverSideHotbar, ContainerInput.SWAP, player)
                }
            }
        },
        PICK {
            override fun NonNullContext.switch(targetSlot: Slot, block: () -> Unit) {
                if (!targetSlot.isHotbarSlot) {
                    SWAP.run {
                        switch(targetSlot, block)
                        return
                    }
                }
                val number = menuSlotId(targetSlot)
                if (number < 0) return
                interaction.handleContainerInput(player.containerMenu.containerId, number, 0, ContainerInput.PICKUP, player)
                block.invoke()
                interaction.handleContainerInput(player.containerMenu.containerId, number, 0, ContainerInput.PICKUP, player)
            }
        };

        abstract fun NonNullContext.switch(targetSlot: Slot, block: () -> Unit)
    }

    enum class Override : Displayable {
        DEFAULT {
            override val mode get() = ClientSettings.ghostHandBypass as BypassMode
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
