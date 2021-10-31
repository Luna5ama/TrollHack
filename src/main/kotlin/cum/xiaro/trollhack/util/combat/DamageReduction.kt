package cum.xiaro.trollhack.util.combat

import cum.xiaro.trollhack.module.modules.combat.CombatSetting
import cum.xiaro.trollhack.util.items.getEnchantmentLevel
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.init.Enchantments
import net.minecraft.init.MobEffects
import net.minecraft.util.CombatRules
import kotlin.math.max
import kotlin.math.min

class DamageReduction(val entity: EntityLivingBase) {
    private val armorValue = entity.totalArmorValue.toFloat()
    private val toughness = entity.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).attributeValue.toFloat()
    private val resistanceMultiplier: Float
    private val genericMultiplier: Float
    private val blastMultiplier: Float

    init {
        var genericEPF = 0
        var blastEPF = 0

        for (itemStack in entity.armorInventoryList) {
            genericEPF += itemStack.getEnchantmentLevel(Enchantments.PROTECTION)
            blastEPF += itemStack.getEnchantmentLevel(Enchantments.BLAST_PROTECTION) * 2
        }

        resistanceMultiplier = entity.getActivePotionEffect(MobEffects.RESISTANCE)?.let {
            max(1.0f - (it.amplifier + 1) * 0.2f, 0.0f)
        } ?: run {
            if (CombatSetting.assumeResistance) {
                0.8f
            } else {
                1.0f
            }
        }

        genericMultiplier = (1.0f - min(genericEPF, 20) / 25.0f)
        blastMultiplier = (1.0f - min(genericEPF + blastEPF, 20) / 25.0f)
    }

    fun calcDamage(damage: Float, isExplosion: Boolean) =
        CombatRules.getDamageAfterAbsorb(damage, armorValue, toughness) *
            resistanceMultiplier *
            if (isExplosion) blastMultiplier
            else genericMultiplier
}