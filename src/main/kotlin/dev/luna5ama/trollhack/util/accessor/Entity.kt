package dev.luna5ama.trollhack.util.accessor

import dev.luna5ama.trollhack.mixins.accessor.entity.AccessorEntityLivingBase
import net.minecraft.entity.EntityLivingBase

fun EntityLivingBase.onItemUseFinish() {
    (this as AccessorEntityLivingBase).trollInvokeOnItemUseFinish()
}