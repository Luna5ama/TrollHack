package me.luna.trollhack.util.accessor

import net.minecraft.entity.EntityLivingBase

fun EntityLivingBase.onItemUseFinish() {
    (this as me.luna.trollhack.mixins.accessor.entity.AccessorEntityLivingBase).trollInvokeOnItemUseFinish()
}