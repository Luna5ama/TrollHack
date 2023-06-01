package dev.luna5ama.trollhack.module.modules.render

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot

internal object ArmorHide : Module(
    name = "Armor Hide",
    category = Category.RENDER,
    description = "Hides the armor on selected entities",
    visible = false
) {
    private val all by setting("All", true)
    private val player by setting("Player", true, ::all)
    private val armorStand by setting("Armor Stand", true, ::all)
    private val mobs by setting("Mob", true, ::all)
    private val helmet by setting("Helmet", false)
    private val chestplate by setting("Chestplate", false)
    private val leggings by setting("Leggings", false)
    private val boots by setting("Boots", false)

    @JvmStatic
    fun shouldHide(entity: EntityLivingBase, slotIn: EntityEquipmentSlot): Boolean {
        return (all
            || player && entity is EntityPlayer
            || armorStand && entity is EntityArmorStand
            || mobs && entity is EntityMob)
            && shouldHidePiece(slotIn)
    }

    private fun shouldHidePiece(slotIn: EntityEquipmentSlot): Boolean {
        return when (slotIn) {
            EntityEquipmentSlot.HEAD -> !helmet
            EntityEquipmentSlot.CHEST -> !chestplate
            EntityEquipmentSlot.LEGS -> !leggings
            EntityEquipmentSlot.FEET -> !boots
            else -> false
        }
    }
}