package dev.luna5ama.trollhack.util.inventory

import net.minecraft.item.Item
import net.minecraft.item.ItemStack

fun interface ItemStackPredicate {
    operator fun invoke(itemStack: ItemStack): Boolean

    companion object {
        fun byItem(item: Item) = ItemStackPredicate { it.item == item }

        fun byItem(vararg items: Item): ItemStackPredicate {
            val set = items.toSet()
            return ItemStackPredicate { it.item in set }
        }
    }
}