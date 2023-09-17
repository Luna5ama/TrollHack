package dev.luna5ama.trollhack.util.inventory.slot

import dev.luna5ama.trollhack.module.modules.player.Kit
import dev.luna5ama.trollhack.util.inventory.id
import net.minecraft.block.Block
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import java.util.function.Predicate

fun Iterable<Slot>.hasItem(item: Item, predicate: Predicate<ItemStack>) =
    any { slot ->
        slot.stack.let { it.item == item && predicate.test(it) }
    }

inline fun <reified I : Item> Iterable<Slot>.hasItem() =
    any {
        it.stack.item is I
    }

fun Iterable<Slot>.hasItem(item: Item) =
    any {
        it.stack.item == item
    }

fun Iterable<Slot>.hasAnyItem() =
    any {
        !it.stack.isEmpty
    }

fun Iterable<Slot>.hasEmpty() =
    any {
        it.stack.isEmpty
    }

fun Iterable<Slot>.countEmpty() =
    count { it.stack.isEmpty }

inline fun <reified B : Block> Iterable<Slot>.countBlock(predicate: Predicate<ItemStack>? = null) =
    countByStack { itemStack ->
        itemStack.item.let { it is ItemBlock && it.block is B } && (predicate == null || predicate.test(itemStack))
    }

fun Iterable<Slot>.countBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
    countByStack { itemStack ->
        itemStack.item.let { it is ItemBlock && it.block == block } && (predicate == null || predicate.test(itemStack))
    }

inline fun <reified I : Item> Iterable<Slot>.countItem(predicate: Predicate<ItemStack>? = null) =
    countByStack {
        it.item is I && (predicate == null || predicate.test(it))
    }

fun Iterable<Slot>.countItem(item: Item, predicate: Predicate<ItemStack>? = null) =
    countByStack {
        it.item == item && (predicate == null || predicate.test(it))
    }

fun Iterable<Slot>.countID(itemID: Int, predicate: Predicate<ItemStack>? = null) =
    countByStack {
        it.item.id == itemID && (predicate == null || predicate.test(it))
    }

fun Iterable<Slot>.countByStack(predicate: Predicate<ItemStack>? = null) =
    sumOf { slot ->
        slot.stack.let { if (predicate == null || predicate.test(it)) it.count else 0 }
    }


fun <T : Slot> Iterable<T>.firstEmpty() =
    firstByStack {
        it.isEmpty
    }

inline fun <reified B : Block> Iterable<HotbarSlot>.firstBlock(predicate: Predicate<ItemStack>? = null) =
    firstByStack { itemStack ->
        itemStack.item.let { it is ItemBlock && it.block is B } && (predicate == null || predicate.test(itemStack))
    }

inline fun <reified B : Block> Iterable<Slot>.firstBlock(predicate: Predicate<ItemStack>? = null) =
    firstByStack { itemStack ->
        itemStack.item.let { it is ItemBlock && it.block is B } && (predicate == null || predicate.test(itemStack))
    }

fun <T : Slot> Iterable<T>.firstBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
    firstByStack { itemStack ->
        itemStack.item.let { it is ItemBlock && it.block == block } && (predicate == null || predicate.test(itemStack))
    }

inline fun <reified I : Item, T : Slot> Iterable<T>.firstItem(predicate: Predicate<ItemStack>? = null) =
    firstByStack {
        it.item is I && (predicate == null || predicate.test(it))
    }

fun <T : Slot> Iterable<T>.firstItem(item: Item, predicate: Predicate<ItemStack>? = null) =
    firstByStack {
        it.item == item && (predicate == null || predicate.test(it))
    }

fun <T : Slot> Iterable<T>.firstID(itemID: Int, predicate: Predicate<ItemStack>? = null) =
    firstByStack {
        it.item.id == itemID && (predicate == null || predicate.test(it))
    }

fun <T : Slot> Iterable<T>.firstByStack(predicate: Predicate<ItemStack>? = null): T? =
    firstOrNull {
        (predicate == null || predicate.test(it.stack))
    }


inline fun <reified B : Block, T : Slot> Iterable<T>.filterByBlock(predicate: Predicate<ItemStack>? = null) =
    filterByStack { itemStack ->
        itemStack.item.let { it is ItemBlock && it.block is B } && (predicate == null || predicate.test(itemStack))
    }

fun <T : Slot> Iterable<T>.filterByBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
    filterByStack { itemStack ->
        itemStack.item.let { it is ItemBlock && it.block == block } && (predicate == null || predicate.test(itemStack))
    }

inline fun <reified I : Item, T : Slot> Iterable<T>.filterByItem(predicate: Predicate<ItemStack>? = null) =
    filterByStack {
        it.item is I && (predicate == null || predicate.test(it))
    }

fun <T : Slot> Iterable<T>.filterByItem(item: Item, predicate: Predicate<ItemStack>? = null) =
    filterByStack {
        it.item == item && (predicate == null || predicate.test(it))
    }

fun <T : Slot> Iterable<T>.filterByID(itemID: Int, predicate: Predicate<ItemStack>? = null) =
    filterByStack {
        it.item.id == itemID && (predicate == null || predicate.test(it))
    }

fun <T : Slot> Iterable<T>.filterByStack(predicate: Predicate<ItemStack>? = null) =
    filter {
        predicate == null || predicate.test(it.stack)
    }

fun Iterable<Slot>.getCompatibleStack(slotTo: Slot): Slot? {
    var maxSlot: Slot? = null
    var maxSize = 0

    val stackTo = slotTo.stack
    val isEmpty = stackTo.isEmpty
    val neededSize = if (isEmpty) 64 else stackTo.maxStackSize - stackTo.count
    if (neededSize <= 0) return null

    for (slotFrom in this) {
        if (slotFrom.slotNumber == slotTo.slotNumber) continue

        val stackFrom = slotFrom.stack
        val size = stackFrom.count

        if (!stackTo.isItemEqual(stackFrom)) continue
        if (!ItemStack.areItemStackTagsEqual(stackTo, stackFrom)) continue
        if (size == neededSize) return slotFrom

        if (size == stackFrom.maxStackSize) {
            return slotFrom
        } else if (size > maxSize) {
            maxSlot = slotFrom
            maxSize = size
        }
    }

    return maxSlot
}


fun Iterable<Slot>.findMaxCompatibleStack(slotTo: Slot): Slot? {
    return findMaxCompatibleStack(slotTo, slotTo.stack.item)
}

internal fun Iterable<Slot>.findMaxCompatibleStack(slotTo: Slot, targetItem: Kit.ItemEntry): Slot? {
    var maxSlot: Slot? = null
    var maxSize = 0

    val stackTo = slotTo.stack
    val isEmpty = stackTo.isEmpty
    val neededSize = if (isEmpty) 64 else stackTo.maxStackSize - stackTo.count
    if (neededSize <= 0) return null

    for (slotFrom in this) {
        if (slotFrom.slotNumber == slotTo.slotNumber) continue

        val stackFrom = slotFrom.stack
        if (!targetItem.equals(stackFrom)) continue

        val size = stackFrom.count
        if (!isEmpty && targetItem.equals(stackFrom)) {
            if (!stackTo.isItemEqual(stackFrom)) continue
            if (!ItemStack.areItemStackTagsEqual(stackTo, stackFrom)) continue
            if (size == neededSize) return slotFrom
        }

        if (size == stackFrom.maxStackSize) {
            return slotFrom
        } else if (size > maxSize) {
            maxSlot = slotFrom
            maxSize = size
        }
    }

    return maxSlot ?: findMaxCompatibleStack(slotTo, targetItem.item)
}

fun Iterable<Slot>.findMaxCompatibleStack(slotTo: Slot, targetItem: Item): Slot? {
    var maxSlot: Slot? = null
    var maxSize = 0

    val stackTo = slotTo.stack
    val isEmpty = stackTo.isEmpty
    val neededSize = if (isEmpty) 64 else stackTo.maxStackSize - stackTo.count
    if (neededSize <= 0) return null

    for (slotFrom in this) {
        if (slotFrom.slotNumber == slotTo.slotNumber) continue

        val stackFrom = slotFrom.stack
        if (stackFrom.item != targetItem) continue

        val size = stackFrom.count
        if (!isEmpty && stackTo.item == targetItem) {
            if (!stackTo.isItemEqual(stackFrom)) continue
            if (!ItemStack.areItemStackTagsEqual(stackTo, stackFrom)) continue
            if (size == neededSize) return slotFrom
        }

        if (size == stackFrom.maxStackSize) {
            return slotFrom
        } else if (size > maxSize) {
            maxSlot = slotFrom
            maxSize = size
        }
    }

    return maxSlot
}


fun Iterable<Slot>.findFirstCompatibleStack(slotTo: Slot): Slot? {
    return findFirstCompatibleStack(slotTo, slotTo.stack.item)
}

internal fun Iterable<Slot>.findFirstCompatibleStack(slotTo: Slot, targetItem: Kit.ItemEntry): Slot? {
    val stackTo = slotTo.stack
    val isEmpty = stackTo.isEmpty
    val neededSize = if (isEmpty) 64 else stackTo.maxStackSize - stackTo.count
    if (neededSize <= 0) return null

    for (slotFrom in this) {
        if (slotFrom.slotNumber == slotTo.slotNumber) continue

        val stackFrom = slotFrom.stack
        if (!targetItem.equals(stackFrom)) continue

        if (!isEmpty && targetItem.equals(stackFrom)) {
            if (!stackTo.isItemEqual(stackFrom)) continue
            if (!ItemStack.areItemStackTagsEqual(stackTo, stackFrom)) continue
        }

        return slotFrom
    }

    return findFirstCompatibleStack(slotTo, targetItem.item)
}

fun Iterable<Slot>.findFirstCompatibleStack(slotTo: Slot, targetItem: Item): Slot? {
    val stackTo = slotTo.stack
    val isEmpty = stackTo.isEmpty
    val neededSize = if (isEmpty) 64 else stackTo.maxStackSize - stackTo.count
    if (neededSize <= 0) return null

    for (slotFrom in this) {
        if (slotFrom.slotNumber == slotTo.slotNumber) continue

        val stackFrom = slotFrom.stack
        if (stackFrom.item != targetItem) continue

        if (!isEmpty && stackTo.item == targetItem) {
            if (!stackTo.isItemEqual(stackFrom)) continue
            if (!ItemStack.areItemStackTagsEqual(stackTo, stackFrom)) continue
        }

        return slotFrom
    }

    return null
}
