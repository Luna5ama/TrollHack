/*
 * Copyright (c) 2021-2022, SagiriXiguajerry. All rights reserved.
 * This repository will be transformed to SuperMic_233.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package dev.luna5ama.trollhack.utils.inventory

import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.utils.NonNullContext
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import java.util.*


@Deprecated("See AutoCrystal for advanced inventory swapping.")
internal object InventoryUtils {

    val shiftBlocks: List<Block> = listOf(
        Blocks.ENDER_CHEST,
        Blocks.CHEST,
        Blocks.TRAPPED_CHEST,
        Blocks.CRAFTING_TABLE,
        Blocks.BIRCH_TRAPDOOR,
        Blocks.BAMBOO_TRAPDOOR,
        Blocks.DARK_OAK_TRAPDOOR,
        Blocks.CHERRY_TRAPDOOR,
        Blocks.ANVIL,
        Blocks.BREWING_STAND,
        Blocks.HOPPER,
        Blocks.DROPPER,
        Blocks.DISPENSER,
        Blocks.ACACIA_TRAPDOOR,
        Blocks.ENCHANTING_TABLE,
        Blocks.WHITE_SHULKER_BOX,
        Blocks.ORANGE_SHULKER_BOX,
        Blocks.MAGENTA_SHULKER_BOX,
        Blocks.LIGHT_BLUE_SHULKER_BOX,
        Blocks.YELLOW_SHULKER_BOX,
        Blocks.LIME_SHULKER_BOX,
        Blocks.PINK_SHULKER_BOX,
        Blocks.GRAY_SHULKER_BOX,
        Blocks.CYAN_SHULKER_BOX,
        Blocks.PURPLE_SHULKER_BOX,
        Blocks.BLUE_SHULKER_BOX,
        Blocks.BROWN_SHULKER_BOX,
        Blocks.GREEN_SHULKER_BOX,
        Blocks.RED_SHULKER_BOX,
        Blocks.BLACK_SHULKER_BOX
    )

    var lastSlot = -1
    var lastSelect = -1

    context(NonNullContext)
    fun doSwap(slot: Int) {
        player.inventory.selectedSlot = slot
        netHandler.send(ServerboundSetCarriedItemPacket(slot))
    }

    fun holdingItem(clazz: Class<*>): Boolean {
        var result: Boolean
        val stack = mc.player!!.mainHandItem
        result = isInstanceOf(stack, clazz)
        if (!result) {
            result = isInstanceOf(stack, clazz)
        }
        return result
    }

    fun isInstanceOf(stack: ItemStack, clazz: Class<*>): Boolean {
        val item = stack.item
        if (clazz.isInstance(item)) {
            return true
        }
        if (item is BlockItem) {
            val block = Block.byItem(item)
            return clazz.isInstance(block)
        }
        return false
    }

    fun getStackInSlot(i: Int): ItemStack {
        return mc.player!!.inventory.getItem(i)
    }

    fun findItem(input: Item): Int {
        for (i in 0..8) {
            val item = getStackInSlot(i).item
            if (Item.getId(item) != Item.getId(input)) continue
            return i
        }
        return -1
    }

    fun findClass(clazz: Class<*>): Int {
        for (i in 0..8) {
            val stack = getStackInSlot(i)
            if (stack == ItemStack.EMPTY) continue
            if (clazz.isInstance(stack.item)) {
                return i
            }
            if (stack.item !is BlockItem || !clazz.isInstance((stack.item as BlockItem).block)) continue
            return i
        }
        return -1
    }

    fun findClassInventorySlot(clazz: Class<*>): Int {
        for (i in 0..44) {
            val stack = mc.player!!.inventory.getItem(i)
            if (stack == ItemStack.EMPTY) continue
            if (clazz.isInstance(stack.item)) {
                return if (i < 9) i + 36 else i
            }
            if (stack.item !is BlockItem || !clazz.isInstance((stack.item as BlockItem).block)) continue
            return if (i < 9) i + 36 else i
        }
        return -1
    }

    fun findBlock(blockIn: Block): Int {
        for (i in 0..8) {
            val stack = getStackInSlot(i)
            if (stack == ItemStack.EMPTY || stack.item !is BlockItem || (stack.item as BlockItem).block != blockIn)
                continue
            return i
        }
        return -1
    }

    fun nofindBlock(blockIn: Block): Int {
        for (i in 0..8) {
            val stack = getStackInSlot(i)
            if (stack == ItemStack.EMPTY || stack.item !is BlockItem || (stack.item as BlockItem).block == blockIn)
                continue
            return i
        }
        return -1
    }

    fun findBlock45(blockIn: Block): Int {
        for (i in 0..45) {
            val stack = getStackInSlot(i)
            if (stack == ItemStack.EMPTY || stack.item !is BlockItem || (stack.item as BlockItem).block != blockIn)
                continue
            return i
        }
        return -1
    }

    fun findBlock9(blockIn:  Item): Int {
        for (i in 9..36) {
            val stack = getStackInSlot(i)
            if (stack == ItemStack.EMPTY || stack.item !is BlockItem || (stack.item as BlockItem).block != blockIn)
                continue
            return i
        }
        return -1
    }

    fun findUnBlock(): Int {
        for (i in 0..8) {
            val stack = getStackInSlot(i)
            if (stack.item is BlockItem) continue
            return i
        }
        return -1
    }

    fun findBlock(): Int {
        for (i in 0..8) {
            val stack = getStackInSlot(i)
            if ((stack.item is BlockItem && !shiftBlocks.contains(
                    Block.byItem(
                        stack.item
                    )
                )) && (stack.item as BlockItem).block != Blocks.COBWEB
            ) return i
        }
        return -1
    }

    fun findBlockInventorySlot(block: Block?): Int {
        return block?.let { findItemInventorySlot(Item.byBlock(it)) } ?: -1
    }

    fun findItemInventorySlot(item: Item): Int {
        for (i in 0..44) {
            val stack = mc.player!!.inventory.getItem(i)
            if (stack.item == item) return if (i < 9) i + 36 else i
        }
        return -1
    }
    fun findItemInventorySlot9(item: Item): Int {
        for (i in 9..35) {
            val stack = mc.player!!.inventory.getItem(i)
            if (stack.item == item) return i
        }
        return -1
    }

    fun getInventoryAndHotbarSlots(): Map<Int, ItemStack> {
        val fullInventorySlots = HashMap<Int, ItemStack>()

        for (current in 0..44) {
            fullInventorySlots[current] = mc.player!!.inventory.getItem(current)
        }

        return fullInventorySlots
    }

    context(NonNullContext)
    fun inventorySwap(slot: Int, selectedSlot: Int) {
        if (slot == lastSlot) {
            doSwap(lastSelect)
            lastSlot = -1
            lastSelect = -1
            return
        }
        if (slot - 36 == selectedSlot) return
        if (ClientSettings.inventorySwapBypass) {
            if (slot - 36 >= 0) {
                lastSlot = slot
                lastSelect = selectedSlot
                doSwap(slot - 36)
                return
            }

            interaction.handleInventoryMouseClick(
                player.containerMenu.containerId,
                slot,
                0,
                ClickType.PICKUP,
                player
            )
        } else interaction.handleInventoryMouseClick(
            player.containerMenu.containerId,
            slot,
            selectedSlot,
            ClickType.SWAP,
            player
        )
    }


}
