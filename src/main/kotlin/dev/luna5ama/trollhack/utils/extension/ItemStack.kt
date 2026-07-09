package dev.luna5ama.trollhack.utils.extension

import net.minecraft.world.item.ItemStack
import kotlin.math.max

val ItemStack.getDamagePercent: Int
    get() = ((this.maxDamage - this.damageValue) / max(0.1, this.maxDamage.toDouble()) * 100.0f).toInt()