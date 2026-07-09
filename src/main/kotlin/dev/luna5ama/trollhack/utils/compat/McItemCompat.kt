package dev.luna5ama.trollhack.utils.compat

import net.minecraft.core.component.DataComponents
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

val ItemStack.isSwordCompat: Boolean
    get() = this.`is`(ItemTags.SWORDS)

val ItemStack.isToolCompat: Boolean
    get() = has(DataComponents.TOOL)

val LivingEntity.armorStacksCompat: List<ItemStack>
    get() = listOf(
        getItemBySlot(EquipmentSlot.FEET),
        getItemBySlot(EquipmentSlot.LEGS),
        getItemBySlot(EquipmentSlot.CHEST),
        getItemBySlot(EquipmentSlot.HEAD)
    )
