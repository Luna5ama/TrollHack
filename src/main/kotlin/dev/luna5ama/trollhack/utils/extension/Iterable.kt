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

package dev.luna5ama.trollhack.utils.extension

import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import java.util.function.Predicate

fun Iterable<Slot>.hasItem(item: Item, predicate: Predicate<ItemStack>) =
    any { slot ->
        slot.item.let { it.item == item && predicate.test(it) }
    }

inline fun <reified I : Item> Iterable<Slot>.hasItem() =
    any {
        it.item.item is I
    }

fun Iterable<Slot>.hasItem(item: Item) =
    any {
        it.item.item == item
    }

fun Iterable<Slot>.hasAnyItem() =
    any {
        !it.item.isEmpty
    }

fun Iterable<Slot>.hasEmpty() =
    any {
        it.item.isEmpty
    }

fun Iterable<Slot>.countEmpty() =
    count { it.item.isEmpty }

inline fun <reified B : Block> Iterable<Slot>.countBlock(predicate: Predicate<ItemStack>? = null) =
    countByStack { itemStack ->
        itemStack.item.let { it is BlockItem && it.block is B } && (predicate == null || predicate.test(itemStack))
    }

fun Iterable<Slot>.countBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
    countByStack { itemStack ->
        itemStack.item.let { it is BlockItem && it.block == block } && (predicate == null || predicate.test(itemStack))
    }

inline fun <reified I : Item> Iterable<Slot>.countItem(predicate: Predicate<ItemStack>? = null) =
    countByStack {
        it.item is I && (predicate == null || predicate.test(it))
    }

fun Iterable<Slot>.countItem(item: Item, predicate: Predicate<ItemStack>? = null) =
    countByStack {
        it.item == item && (predicate == null || predicate.test(it))
    }

//fun Iterable<Slot>.countID(itemID: Int, predicate: Predicate<ItemStack>? = null) =
//    countByStack {
//        it.item.id == itemID && (predicate == null || predicate.test(it))
//    }

fun Iterable<Slot>.countByStack(predicate: Predicate<ItemStack>? = null) =
    sumOf { slot ->
        slot.item.let { if (predicate == null || predicate.test(it)) it.count else 0 }
    }


fun <T : Slot> Iterable<T>.firstEmpty() =
    firstByStack {
        it.isEmpty
    }

inline fun <reified B : Block, T : Slot> Iterable<T>.firstBlock(predicate: Predicate<ItemStack>? = null) =
    firstByStack { itemStack ->
        itemStack.item.let { it is BlockItem && it.block is B } && (predicate == null || predicate.test(itemStack))
    }

fun <T : Slot> Iterable<T>.firstBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
    firstByStack { itemStack ->
        itemStack.item.let { it is BlockItem && it.block == block } && (predicate == null || predicate.test(itemStack))
    }

inline fun <reified I : Item, T : Slot> Iterable<T>.firstItem(predicate: Predicate<ItemStack>? = null) =
    firstByStack {
        it.item is I && (predicate == null || predicate.test(it))
    }

fun <T : Slot> Iterable<T>.firstItem(item: Item, predicate: Predicate<ItemStack>? = null) =
    firstByStack {
        it.item == item && (predicate == null || predicate.test(it))
    }

//fun <T : Slot> Iterable<T>.firstID(itemID: Int, predicate: Predicate<ItemStack>? = null) =
//    firstByStack {
//        it.item.id == itemID && (predicate == null || predicate.test(it))
//    }

fun <T : Slot> Iterable<T>.firstByStack(predicate: Predicate<ItemStack>? = null): T? =
    firstOrNull {
        (predicate == null || predicate.test(it.item))
    }


inline fun <reified B : Block, T : Slot> Iterable<T>.filterByBlock(predicate: Predicate<ItemStack>? = null) =
    filterByStack { itemStack ->
        itemStack.item.let { it is BlockItem && it.block is B } && (predicate == null || predicate.test(itemStack))
    }

fun <T : Slot> Iterable<T>.filterByBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
    filterByStack { itemStack ->
        itemStack.item.let { it is BlockItem && it.block == block } && (predicate == null || predicate.test(itemStack))
    }

inline fun <reified I : Item, T : Slot> Iterable<T>.filterByItem(predicate: Predicate<ItemStack>? = null) =
    filterByStack {
        it.item is I && (predicate == null || predicate.test(it))
    }

fun <T : Slot> Iterable<T>.filterByItem(item: Item, predicate: Predicate<ItemStack>? = null) =
    filterByStack {
        it.item == item && (predicate == null || predicate.test(it))
    }

//fun <T : Slot> Iterable<T>.filterByID(itemID: Int, predicate: Predicate<ItemStack>? = null) =
//    filterByStack {
//        it.item.id == itemID && (predicate == null || predicate.test(it))
//    }

fun <T : Slot> Iterable<T>.filterByStack(predicate: Predicate<ItemStack>? = null) =
    filter {
        predicate == null || predicate.test(it.item)
    }

operator fun <T> List<T>.component6() = this[5]
operator fun <T> List<T>.component7() = this[6]
operator fun <T> List<T>.component8() = this[7]
operator fun <T> List<T>.component9() = this[8]
