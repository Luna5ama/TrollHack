package dev.luna5ama.trollhack.utils.inventory

import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.extension.firstBlock
import dev.luna5ama.trollhack.utils.extension.firstItem
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import java.util.function.Predicate

/**
 * Try to swap selected hotbar slot to [B] that matches with [predicate]
 */
inline fun <reified B : Block> NonNullContext.swapToBlock(predicate: Predicate<ItemStack>? = null): Boolean {
    return player.hotbarSlots.firstBlock<B, HotbarSlot>(predicate)?.let {
        swapToSlot(it)
        true
    } ?: false
}

/**
 * Try to swap selected hotbar slot to [block] that matches with [predicate]
 */
fun NonNullContext.swapToBlock(block: Block, predicate: Predicate<ItemStack>? = null): Boolean {
    return player.hotbarSlots.firstBlock(block, predicate)?.let {
        swapToSlot(it)
        true
    } ?: false
}

/**
 * Try to swap selected hotbar slot to [I] that matches with [predicate]
 */
inline fun <reified I : Item> NonNullContext.swapToItem(predicate: Predicate<ItemStack>? = null): Boolean {
    return player.hotbarSlots.firstItem<I, HotbarSlot>(predicate)?.let {
        swapToSlot(it)
        true
    } == true
}

/**
 * Try to swap selected hotbar slot to [item] that matches with [predicate]
 */
fun NonNullContext.swapToItem(item: Item, predicate: Predicate<ItemStack>? = null): Boolean {
    return player.hotbarSlots.firstItem(item, predicate)?.let {
        swapToSlot(it)
        true
    } == true
}

/**
 * Swap the selected hotbar slot to [hotbarSlot]
 */
fun NonNullContext.swapToSlot(hotbarSlot: HotbarSlot) {
    swapToSlot(hotbarSlot.hotbarSlot - 36)
}

/**
 * Swap the selected hotbar slot to [slot]
 */
fun NonNullContext.swapToSlot(slot: Int) {
    if (slot !in SlotRanges.HOTBAR) return
    player.inventory.selectedSlot = slot
    netHandler.send(ServerboundSetCarriedItemPacket(slot))
    interaction.ensureHasSentCarriedItem()
}