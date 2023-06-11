package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.inventory.*
import dev.luna5ama.trollhack.util.inventory.operation.action
import dev.luna5ama.trollhack.util.inventory.operation.pickUp
import dev.luna5ama.trollhack.util.inventory.operation.quickMove
import dev.luna5ama.trollhack.util.inventory.operation.swapWith
import dev.luna5ama.trollhack.util.inventory.slot.*
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.init.Enchantments
import net.minecraft.init.Items
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.util.EnumHand
import kotlin.math.max
import kotlin.math.roundToInt

internal object AutoArmor : Module(
    name = "Auto Armor",
    category = Category.COMBAT,
    description = "Automatically equips armour",
    modulePriority = 500
) {
    private val runInGui by setting("Run In GUI", true)
    private val antiGlitchArmor by setting("Anti Glitch Armor", true)
    private val stackedArmor by setting("Stacked Armor", false)
    private val swapSlot by setting("Swap Slot", 9, 1..9, 1, { stackedArmor })
    private val blastProtectionLeggings by setting(
        "Blast Protection Leggings",
        true,
        description = "Prefer leggings with blast protection enchantment"
    )
    private val armorSaver by setting(
        "Armor Saver",
        false,
        { !stackedArmor },
        description = "Swaps out armor at low durability"
    )
    private val duraThreshold by setting("Durability Threshold", 10, 1..50, 1, { !stackedArmor && armorSaver })
    private val delay by setting("Delay", 2, 1..10, 1)

    private val moveTimer = TickTimer(TimeUnit.TICKS)
    private var lastTask: InventoryTask? = null

    init {
        safeParallelListener<TickEvent.Post> {
            if (AutoMend.isActive() || (!runInGui && mc.currentScreen is GuiContainer) || !lastTask.executedOrTrue) return@safeParallelListener

            val armorSlots = player.armorSlots
            val isElytraOn = player.chestSlot.stack.item == Items.ELYTRA

            // store slots and values of best armor pieces, initialize with currently equipped armor
            // Pair<Slot, Value>
            val bestArmors = Array(4) {
                armorSlots[it] to getArmorValue(armorSlots[it].stack)
            }

            // search inventory for better armor
            findBestArmor(player.hotbarSlots, bestArmors, isElytraOn)
            findBestArmor(player.craftingSlots, bestArmors, isElytraOn)
            findBestArmor(player.inventorySlots, bestArmors, isElytraOn)

            // equip better armor
            if (equipArmor(armorSlots, bestArmors)) {
                moveTimer.reset()
            } else if (antiGlitchArmor && moveTimer.tick(10) && player.totalArmorValue != player.armorSlots.sumOf {
                    getRawArmorValue(
                        it.stack
                    )
                }) {
                inventoryTask {
                    for (slot in player.armorSlots) {
                        pickUp(slot)
                        pickUp(slot)
                    }
                    delay(1, TimeUnit.TICKS)
                    postDelay(delay, TimeUnit.TICKS)
                    runInGui()
                }
                moveTimer.reset()
            }
        }
    }

    private fun getRawArmorValue(itemStack: ItemStack): Int {
        val item = itemStack.item
        return if (item !is ItemArmor) 0 else item.damageReduceAmount
    }

    private fun findBestArmor(slots: List<Slot>, bestArmors: Array<Pair<Slot, Float>>, isElytraOn: Boolean) {
        for (slot in slots) {
            val itemStack = slot.stack
            val item = itemStack.item
            if (item !is ItemArmor) continue

            val armorType = item.armorType
            if (armorType == EntityEquipmentSlot.CHEST && isElytraOn) continue // Skip if item is chestplate and we have elytra equipped

            val armorValue = getArmorValue(itemStack)
            val armorIndex = 3 - armorType.index

            if (armorValue > bestArmors[armorIndex].second) bestArmors[armorIndex] = slot to armorValue
        }
    }

    private fun getArmorValue(itemStack: ItemStack): Float {
        val item = itemStack.item
        return if (item !is ItemArmor) {
            -1.0f
        } else {
            val value = item.damageReduceAmount * getProtectionModifier(itemStack)

            // Less weight for armor with low dura so it gets swaps out
            if (!stackedArmor && armorSaver && itemStack.isItemStackDamageable && itemStack.duraPercentage < duraThreshold) value * 0.1f
            else value
        }
    }

    private val ItemStack.duraPercentage: Int
        get() = (this.durability / this.maxDamage.toFloat() * 100.0f).roundToInt()

    private fun getProtectionModifier(itemStack: ItemStack): Float {
        val item = itemStack.item
        val protectionLevel = itemStack.getEnchantmentLevel(Enchantments.PROTECTION)

        val level = if (blastProtectionLeggings && item is ItemArmor && item.armorType == EntityEquipmentSlot.LEGS) {
            // Blast protection reduces 2x damage
            max(itemStack.getEnchantmentLevel(Enchantments.BLAST_PROTECTION) * 2, protectionLevel)
        } else {
            protectionLevel
        }

        return 1.0f + 0.04f * level
    }

    private fun SafeClientEvent.equipArmor(armorSlots: List<Slot>, bestArmors: Array<Pair<Slot, Float>>): Boolean {
        for ((index, pair) in bestArmors.withIndex()) {
            val slotFrom = pair.first
            if (slotFrom.slotNumber in 5..8) continue // Skip if we didn't find a better armor in inventory

            val slotTo = armorSlots[index]

            lastTask = if (stackedArmor && slotFrom.stack.count > 1) {
                moveStackedArmor(slotFrom, slotTo)
            } else {
                if (pair.first.slotNumber in 1..4) {
                    moveFromCraftingSlot(pair.first, slotTo)
                } else {
                    moveFromInventory(pair.first, slotTo)
                }
            }

            return true // Don't move more than one at once
        }

        return false
    }

    private fun SafeClientEvent.moveStackedArmor(slotFrom: Slot, slotTo: Slot): InventoryTask {
        return slotFrom.toHotbarSlotOrNull()?.let {
            if (slotTo.hasStack) {
                inventoryTask {
                    pickUp(slotTo)
                    action {
                        ghostSwitch(it) {
                            connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
                        }
                    }
                    pickUp(slotFrom)
                    postDelay(delay, TimeUnit.TICKS)
                    runInGui()
                }
            } else {
                inventoryTask {
                    action {
                        ghostSwitch(it) {
                            connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
                        }
                    }
                    postDelay(delay, TimeUnit.TICKS)
                    runInGui()
                }
            }
        } ?: run {
            inventoryTask {
                swapWith(slotFrom, player.getHotbarSlot(swapSlot - 1))
                postDelay(delay, TimeUnit.TICKS)
                runInGui()
            }
        }
    }

    private fun moveFromCraftingSlot(slotFrom: Slot, slotTo: Slot): InventoryTask {
        return if (!slotTo.hasStack) {
            inventoryTask {
                pickUp(slotFrom) // Pick up the new one
                pickUp(slotTo) // Put the new one into armor slot
                postDelay(delay, TimeUnit.TICKS)
                runInGui()
            }
        } else {
            inventoryTask {
                pickUp(slotFrom) // Pick up the new one
                pickUp(slotTo) // Put the new one into armor slot
                pickUp(slotFrom) // Put the old one into the empty slot
                postDelay(delay, TimeUnit.TICKS)
                runInGui()
            }
        }
    }

    private fun SafeClientEvent.moveFromInventory(slotFrom: Slot, slotTo: Slot): InventoryTask {
        return when {
            !slotTo.hasStack -> {
                inventoryTask {
                    quickMove(slotFrom) // Move the new one into armor slot)
                    postDelay(delay, TimeUnit.TICKS)
                    runInGui()
                }
            }

            player.inventorySlots.hasEmpty() -> {
                inventoryTask {
                    quickMove(slotTo) // Move out the old one
                    quickMove(slotFrom) // Put the old one into the empty slot
                    postDelay(delay, TimeUnit.TICKS)
                    runInGui()
                }
            }

            else -> {
                inventoryTask {
                    pickUp(slotFrom) // Pick up the new one
                    pickUp(slotTo) // Put the new one into armor slot
                    postDelay(delay, TimeUnit.TICKS)
                    runInGui()
                }
            }
        }
    }
}