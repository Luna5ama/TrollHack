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

package dev.luna5ama.trollhack.utils.world.explosion.advanced

import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.compat.armorStacksCompat
import dev.luna5ama.trollhack.utils.extension.getEnchantmentLevel
import dev.luna5ama.trollhack.utils.timing.TickTimer
import dev.luna5ama.trollhack.utils.world.CombatRules
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.enchantment.Enchantments
import kotlin.math.max
import kotlin.math.min

class DamageReduction(val entity: LivingEntity, val updateDelay: Long = 100) {
    private val armorValue = entity.armorValue.toFloat()
    private val toughness = entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS).toFloat()
    private var resistanceMultiplier: Float
    private var genericMultiplier: Float
    private var blastMultiplier: Float
    private val updateTimer = TickTimer()

    init {
        var genericEPF = 0
        var blastEPF = 0

        for (itemStack in entity.armorStacksCompat) {
            genericEPF += itemStack.getEnchantmentLevel(Enchantments.PROTECTION)
            blastEPF += itemStack.getEnchantmentLevel(Enchantments.BLAST_PROTECTION) * 2
        }

        resistanceMultiplier = entity.getEffect(MobEffects.RESISTANCE)?.let {
            max(1.0f - (it.amplifier + 1) * 0.2f, 0.0f)
        } ?: run {
            if (ClientSettings.assumeResistance) {
                0.8f
            } else {
                1.0f
            }
        }

        genericMultiplier = (1.0f - min(genericEPF, 20) / 25.0f)
        blastMultiplier = (1.0f - min(genericEPF + blastEPF, 20) / 25.0f)
    }

    fun calcDamage(damage: Float, isExplosion: Boolean): Float {
        if (updateTimer.tickAndReset(updateDelay)) update()
        return CombatRules.getDamageAfterAbsorb(damage, armorValue, toughness) *
                resistanceMultiplier *
                if (isExplosion) blastMultiplier
                else genericMultiplier
    }

    context (NonNullContext)
    private fun getDamageMultiplied(damage: Float): Float {
        val diff = world.difficulty.id
        return damage * if (diff == 0) 0f else if (diff == 2) 1f else if (diff == 1) 0.5f else 1.5f
    }

    private fun update() {
        var genericEPF = 0
        var blastEPF = 0

        for (itemStack in entity.armorStacksCompat) {
            genericEPF += itemStack.getEnchantmentLevel(Enchantments.PROTECTION)
            blastEPF += itemStack.getEnchantmentLevel(Enchantments.BLAST_PROTECTION) * 2
        }

        resistanceMultiplier = entity.getEffect(MobEffects.RESISTANCE)?.let {
            max(1.0f - (it.amplifier + 1) * 0.2f, 0.0f)
        } ?: run {
            if (ClientSettings.assumeResistance) {
                0.8f
            } else {
                1.0f
            }
        }

        genericMultiplier = (1.0f - min(genericEPF, 20) / 25.0f)
        blastMultiplier = (1.0f - min(genericEPF + blastEPF, 20) / 25.0f)
    }
}
