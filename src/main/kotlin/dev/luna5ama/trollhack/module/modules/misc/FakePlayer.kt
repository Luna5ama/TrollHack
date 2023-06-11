package dev.luna5ama.trollhack.module.modules.misc

import com.mojang.authlib.GameProfile
import dev.luna5ama.trollhack.command.CommandManager
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.GuiEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.viewEntity
import dev.luna5ama.trollhack.util.MovementUtils
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.text.formatValue
import dev.luna5ama.trollhack.util.threads.onMainThread
import dev.luna5ama.trollhack.util.threads.runSafe
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.gui.GuiGameOver
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Enchantments
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.item.ItemStack
import net.minecraft.potion.PotionEffect
import org.lwjgl.input.Keyboard
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

internal object FakePlayer : Module(
    name = "Fake Player",
    description = "Spawns a client sided fake player",
    category = Category.MISC
) {
    private val copyInventory by setting("Copy Inventory", true)
    private val copyPotions by setting("Copy Potions", true)
    private val maxArmor by setting("Max Armor", false)
    private val gappleEffects by setting("Gapple Effects", false)
    private val playerName by setting("Player Name", "Player")
    private val arrowMove by setting("Arrow Move", false)
    private val arrowMoveSpeed by setting("Arrow Move Speed", 0.1f, 0.01f..2.0f, 0.01f)

    private const val ENTITY_ID = -696969420
    private var fakePlayer: EntityOtherPlayerMP? = null

    override fun getHudInfo(): String {
        return playerName
    }

    init {
        onEnable {
            runSafe {
                if (playerName == "Player") {
                    NoSpamMessage.sendMessage("You can use ${formatValue("${CommandManager.prefix}set FakePlayer PlayerName <name>")} to set a custom name")
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

        safeParallelListener<TickEvent.Post> {
            val fakePlayer = fakePlayer ?: return@safeParallelListener
            if (!arrowMove) return@safeParallelListener

            val movementInput = MovementUtils.calcMovementInput(
                Keyboard.isKeyDown(Keyboard.KEY_UP),
                Keyboard.isKeyDown(Keyboard.KEY_DOWN),
                Keyboard.isKeyDown(Keyboard.KEY_LEFT),
                Keyboard.isKeyDown(Keyboard.KEY_RIGHT),
                Keyboard.isKeyDown(Keyboard.KEY_RSHIFT),
                Keyboard.isKeyDown(Keyboard.KEY_RCONTROL),
            )
            val moveYaw = MovementUtils.calcMoveYaw(viewEntity.rotationYaw, movementInput.z, movementInput.x)
            val xzMotion = if (movementInput.x == 0.0f && movementInput.z == 0.0f) 0.0f else arrowMoveSpeed
            val yMotion = if (movementInput.y == 0.0f) 0.0f else arrowMoveSpeed

            fakePlayer.setPosition(
                fakePlayer.posX + -sin(moveYaw) * xzMotion,
                fakePlayer.posY + movementInput.y * yMotion,
                fakePlayer.posZ + cos(moveYaw) * xzMotion
            )
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