package dev.luna5ama.trollhack.modules.impl.player

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.gui.TrollClickGui
import dev.luna5ama.trollhack.gui.TrollHudEditor
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.modules.impl.movement.ElytraFlight
import dev.luna5ama.trollhack.modules.impl.movement.ElytraFlightNew
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.utils.extension.getEnchantmentLevel
import dev.luna5ama.trollhack.utils.inventory.hotbarSlots
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantments
import kotlin.math.ceil

object AutoArmor : Module("Auto Armor", category = Category.PLAYER) {
    private val head by setting("Head", EnchantPriority.PROTECTION)
    private val body by setting("Body", EnchantPriority.PROTECTION)
    private val tights by setting("Tights", EnchantPriority.PROTECTION)
    private val feet by setting("Feet", EnchantPriority.PROTECTION)
    private val elytraPriority by setting("Elytra Priority", ElytraPriority.IGNORE)
    private val delay by setting("Delay", 5, 0..10, 1)
    private val oldVersion by setting("Old Version", false)
    private val pauseInventory by setting("Pause Inventory", false)
    private val noMove by setting("No Move", false)
    private val ignoreCurse by setting("Ignore Curse", true)
    private val strict by setting("Strict", false)

    private var cooldown = 0

    init {
        nonNullHandler<TickEvent.Pre> {
            val screen = mc.screen
            if (pauseInventory && screen != null && screen !is ChatScreen && screen !== TrollClickGui && screen !== TrollHudEditor) {
                return@nonNullHandler
            }
            if (cooldown-- > 0) return@nonNullHandler

            for (slot in ARMOR_SLOTS) {
                val current = player.getItemBySlot(slot)
                val currentProtection = protection(current, slot)
                val best = (0..35).asSequence()
                    .map { it to player.inventory.getItem(it) }
                    .filter { (_, stack) -> equipmentSlot(stack) == slot }
                    .map { (index, stack) -> Triple(index, stack, protection(stack, slot)) }
                    .filter { it.third > currentProtection }
                    .maxByOrNull { it.third }
                    ?: continue

                val inventorySlot = if (best.first < 9) best.first + 36 else best.first
                if (best.first < 9 && (currentProtection == -1 || !oldVersion)) {
                    val hotbarSlot = player.hotbarSlots[best.first]
                    ghostSwitch(hotbarSlot) {
                        interaction.useItem(player, InteractionHand.MAIN_HAND)
                    }
                } else {
                    if (noMove && player.deltaMovement.horizontalDistanceSqr() > 0.0001) return@nonNullHandler
                    if (strict && player.isSprinting) {
                        netHandler.send(
                            ServerboundPlayerCommandPacket(
                                player,
                                ServerboundPlayerCommandPacket.Action.STOP_SPRINTING
                            )
                        )
                    }

                    val armorMenuSlot = 8 - slot.index
                    val menu = player.inventoryMenu
                    interaction.handleInventoryMouseClick(menu.containerId, inventorySlot, 0, ClickType.PICKUP, player)
                    interaction.handleInventoryMouseClick(menu.containerId, armorMenuSlot, 0, ClickType.PICKUP, player)
                    if (currentProtection != -1) {
                        interaction.handleInventoryMouseClick(menu.containerId, inventorySlot, 0, ClickType.PICKUP, player)
                    }
                    netHandler.send(ServerboundContainerClosePacket(menu.containerId))
                }

                cooldown = delay
                return@nonNullHandler
            }
        }
    }

    private fun equipmentSlot(stack: ItemStack): EquipmentSlot? {
        if (stack.isEmpty) return null
        val slot = stack.get(DataComponents.EQUIPPABLE)?.slot ?: return null
        return slot.takeIf { it in ARMOR_SLOTS }
    }

    private fun protection(stack: ItemStack, slot: EquipmentSlot): Int {
        if (stack.isEmpty) return -1
        if (equipmentSlot(stack) != slot) return 0

        val isElytra = stack.`is`(Items.ELYTRA)
        var enchantmentScore = 0
        if (isElytra) {
            if (stack.isDamageableItem && stack.damageValue >= stack.maxDamage - 1) return 0
            val elytraFlyActive = elytraPriority == ElytraPriority.ELYTRA_PLUS &&
                (ElytraFlight.isEnabled || ElytraFlightNew.isEnabled)
            val preserveEquipped = elytraPriority == ElytraPriority.IGNORE &&
                mc.player?.getItemBySlot(EquipmentSlot.CHEST)?.`is`(Items.ELYTRA) == true
            if (elytraPriority == ElytraPriority.ALWAYS || elytraFlyActive || preserveEquipped) {
                enchantmentScore = 999
            }
        }

        val preference = when (slot) {
            EquipmentSlot.HEAD -> head
            EquipmentSlot.CHEST -> body
            EquipmentSlot.LEGS -> tights
            EquipmentSlot.FEET -> feet
            else -> return 0
        }
        val protectionMultiplier = if (preference == EnchantPriority.PROTECTION) 2 else 1
        val blastMultiplier = if (preference == EnchantPriority.BLAST) 2 else 1
        enchantmentScore += stack.getEnchantmentLevel(Enchantments.PROTECTION) * protectionMultiplier
        enchantmentScore += stack.getEnchantmentLevel(Enchantments.BLAST_PROTECTION) * blastMultiplier

        if (ignoreCurse && stack.getEnchantmentLevel(Enchantments.BINDING_CURSE) > 0) return -999

        var armor = 0.0
        var toughness = 0.0
        stack.get(DataComponents.ATTRIBUTE_MODIFIERS)?.modifiers?.forEach { entry ->
            when (entry.attribute) {
                Attributes.ARMOR -> armor += entry.modifier.amount
                Attributes.ARMOR_TOUGHNESS -> toughness += entry.modifier.amount
            }
        }
        if (!isElytra && armor <= 0.0 && toughness <= 0.0) return 0
        return ((armor + ceil(toughness)) * 10.0).toInt() + enchantmentScore
    }

    private enum class ElytraPriority(override val displayName: CharSequence) : Displayable {
        NONE("None"),
        ALWAYS("Always"),
        ELYTRA_PLUS("ElytraPlus"),
        IGNORE("Ignore")
    }

    private enum class EnchantPriority(override val displayName: CharSequence) : Displayable {
        BLAST("Blast"),
        PROTECTION("Protection")
    }

    private val ARMOR_SLOTS = arrayOf(
        EquipmentSlot.FEET,
        EquipmentSlot.LEGS,
        EquipmentSlot.CHEST,
        EquipmentSlot.HEAD
    )
}
