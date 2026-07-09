package dev.luna5ama.trollhack.utils.world

import net.minecraft.util.Mth

object CombatRules {
    fun getDamageAfterAbsorb(damage: Float, totalArmor: Float, toughnessAttribute: Float): Float {
        val f = 2.0f + toughnessAttribute / 4.0f
        val f1 = Mth.clamp(totalArmor - damage / f, totalArmor * 0.2f, 20.0f)
        return damage * (1.0f - f1 / 25.0f)
    }

    fun getDamageAfterMagicAbsorb(damage: Float, enchantModifiers: Float): Float {
        val f = Mth.clamp(enchantModifiers, 0.0f, 20.0f)
        return damage * (1.0f - f / 25.0f)
    }
}