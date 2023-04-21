package dev.luna5ama.trollhack.util.accessor

import net.minecraft.entity.EntityLivingBase

fun EntityLivingBase.onItemUseFinish() {
    (this as dev.luna5ama.trollhack.mixins.accessor.entity.AccessorEntityLivingBase).trollInvokeOnItemUseFinish()
}