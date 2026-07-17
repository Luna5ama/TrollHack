package dev.luna5ama.trollhack.utils.extension

import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper

fun ItemStack.getEnchantmentLevel(enchantment: ResourceKey<Enchantment>) =
    EnchantmentHelper.getItemEnchantmentLevel(enchantment.entry, this)

val Item.isFood get() = this.components().get(DataComponents.FOOD) != null
