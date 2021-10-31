package cum.xiaro.trollhack.module.modules.misc

import com.mojang.authlib.GameProfile
import cum.xiaro.trollhack.command.CommandManager
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.ConnectionEvent
import cum.xiaro.trollhack.event.events.GuiEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.text.MessageSendUtils
import cum.xiaro.trollhack.util.text.formatValue
import cum.xiaro.trollhack.util.threads.onMainThread
import cum.xiaro.trollhack.util.threads.runSafe
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.gui.GuiGameOver
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Enchantments
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.item.ItemStack
import net.minecraft.potion.PotionEffect
import java.util.*

internal object FakePlayer : Module(
    name = "FakePlayer",
    description = "Spawns a client sided fake player",
    category = Category.MISC
) {
    private val copyInventory by setting("Copy Inventory", true)
    private val copyPotions by setting("Copy Potions", true)
    private val maxArmor by setting("Max Armor", false)
    private val gappleEffects by setting("Gapple Effects", false)
    val playerName by setting("Player Name", "Player")

    private const val ENTITY_ID = -696969420
    private var fakePlayer: EntityOtherPlayerMP? = null

    override fun getHudInfo(): String {
        return playerName
    }

    init {
        onEnable {
            runSafe {
                if (playerName == "Player") {
                    MessageSendUtils.sendNoSpamChatMessage("You can use ${formatValue("${CommandManager.prefix}set FakePlayer PlayerName <name>")} to set a custom name")
                }
                spawnFakePlayer()
            } ?: disable()
        }

        onDisable {
            onMainThread {
                fakePlayer?.setDead()
                mc.world?.removeEntityFromWorld(ENTITY_ID)
                fakePlayer = null
            }
        }

        listener<ConnectionEvent.Disconnect> {
            disable()
        }

        listener<GuiEvent.Displayed> {
            if (it.screen is GuiGameOver) disable()
        }
    }

    private fun SafeClientEvent.spawnFakePlayer() {
        fakePlayer = EntityOtherPlayerMP(world, GameProfile(UUID.randomUUID(), playerName)).apply {
            val viewEntity = mc.renderViewEntity ?: player
            copyLocationAndAnglesFrom(viewEntity)
            rotationYawHead = viewEntity.rotationYawHead

            if (copyInventory) inventory.copyInventory(player.inventory)
            if (copyPotions) copyPotions(player)
            if (maxArmor) addMaxArmor()
            if (gappleEffects) addGappleEffects()
        }.also {
            onMainThread {
                world.addEntityToWorld(ENTITY_ID, it)
            }
        }
    }

    private fun EntityPlayer.copyPotions(otherPlayer: EntityPlayer) {
        for (potionEffect in otherPlayer.activePotionEffects) {
            addPotionEffectForce(PotionEffect(potionEffect.potion, Int.MAX_VALUE, potionEffect.amplifier))
        }
    }

    private fun EntityPlayer.addMaxArmor() {
        inventory.armorInventory[3] = ItemStack(Items.DIAMOND_HELMET).apply {
            addMaxEnchantment(Enchantments.PROTECTION)
            addMaxEnchantment(Enchantments.UNBREAKING)
            addMaxEnchantment(Enchantments.RESPIRATION)
            addMaxEnchantment(Enchantments.AQUA_AFFINITY)
            addMaxEnchantment(Enchantments.MENDING)
        }

        inventory.armorInventory[2] = ItemStack(Items.DIAMOND_CHESTPLATE).apply {
            addMaxEnchantment(Enchantments.PROTECTION)
            addMaxEnchantment(Enchantments.UNBREAKING)
            addMaxEnchantment(Enchantments.MENDING)
        }

        inventory.armorInventory[1] = ItemStack(Items.DIAMOND_LEGGINGS).apply {
            addMaxEnchantment(Enchantments.BLAST_PROTECTION)
            addMaxEnchantment(Enchantments.UNBREAKING)
            addMaxEnchantment(Enchantments.MENDING)
        }

        inventory.armorInventory[0] = ItemStack(Items.DIAMOND_BOOTS).apply {
            addMaxEnchantment(Enchantments.PROTECTION)
            addMaxEnchantment(Enchantments.FEATHER_FALLING)
            addMaxEnchantment(Enchantments.DEPTH_STRIDER)
            addMaxEnchantment(Enchantments.UNBREAKING)
            addMaxEnchantment(Enchantments.MENDING)
        }
    }

    private fun ItemStack.addMaxEnchantment(enchantment: Enchantment) {
        addEnchantment(enchantment, enchantment.maxLevel)
    }

    private fun EntityPlayer.addGappleEffects() {
        addPotionEffectForce(PotionEffect(MobEffects.REGENERATION, Int.MAX_VALUE, 1))
        addPotionEffectForce(PotionEffect(MobEffects.ABSORPTION, Int.MAX_VALUE, 3))
        addPotionEffectForce(PotionEffect(MobEffects.RESISTANCE, Int.MAX_VALUE, 0))
        addPotionEffectForce(PotionEffect(MobEffects.FIRE_RESISTANCE, Int.MAX_VALUE, 0))
    }

    private fun EntityPlayer.addPotionEffectForce(potionEffect: PotionEffect) {
        addPotionEffect(potionEffect)
        potionEffect.potion.applyAttributesModifiersToEntity(this, this.attributeMap, potionEffect.amplifier)
    }
}