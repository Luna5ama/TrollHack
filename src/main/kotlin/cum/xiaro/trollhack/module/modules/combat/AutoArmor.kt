package cum.xiaro.trollhack.module.modules.combat

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.safeParallelListener
import cum.xiaro.trollhack.manager.managers.HotbarManager.spoofHotbar
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.TimeUnit
import cum.xiaro.trollhack.util.inventory.InventoryTask
import cum.xiaro.trollhack.util.inventory.executedOrTrue
import cum.xiaro.trollhack.util.inventory.inventoryTask
import cum.xiaro.trollhack.util.inventory.operation.action
import cum.xiaro.trollhack.util.inventory.operation.pickUp
import cum.xiaro.trollhack.util.inventory.operation.quickMove
import cum.xiaro.trollhack.util.inventory.operation.swapWith
import cum.xiaro.trollhack.util.inventory.slot.*
import cum.xiaro.trollhack.util.items.durability
import cum.xiaro.trollhack.util.items.getEnchantmentLevel
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
    name = "AutoArmor",
    category = Category.COMBAT,
    description = "Automatically equips armour",
    modulePriority = 500
) {
    private val stackedArmor by setting("Stacked Armor", false)
    private val swapSlot by setting("Swap Slot", 9, 1..9, 1, { stackedArmor })
    private val blastProtectionLeggings by setting("Blast Protection Leggings", true, description = "Prefer leggings with blast protection enchantment")
    private val armorSaver by setting("Armor Saver", false, { !stackedArmor }, description = "Swaps out armor at low durability")
    private val duraThreshold by setting("Durability Threshold", 10, 1..50, 1, { !stackedArmor && armorSaver })
    private val delay by setting("Delay", 2, 1..10, 1)

    private var lastTask: InventoryTask? = null

    init {
        safeParallelListener<TickEvent.Post> {
            if (AutoMend.isActive() || player.openContainer != player.inventoryContainer || !lastTask.executedOrTrue) return@safeParallelListener

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
            equipArmor(armorSlots, bestArmors)
        }
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

    private fun SafeClientEvent.equipArmor(armorSlots: List<Slot>, bestArmors: Array<Pair<Slot, Float>>) {
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

            break // Don't move more than one at once
        }
    }

    private fun SafeClientEvent.moveStackedArmor(slotFrom: Slot, slotTo: Slot): InventoryTask? {
        return slotFrom.toHotbarSlotOrNull()?.let {
            if (slotTo.hasStack) {
                inventoryTask {
                    pickUp(slotTo)
                    action {
                        spoofHotbar(it) {
                            connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
                        }
                    }
                    pickUp(slotFrom)
                }
            } else {
                inventoryTask {
                    action {
                        spoofHotbar(it) {
                            connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
                        }
                    }
                }
            }
        } ?: run {
            inventoryTask {
                swapWith(slotFrom, player.getHotbarSlot(swapSlot - 1))
                postDelay(delay, TimeUnit.TICKS)
            }
        }
    }

    private fun moveFromCraftingSlot(slotFrom: Slot, slotTo: Slot): InventoryTask {
        return if (!slotTo.hasStack) {
            inventoryTask {
                pickUp(slotFrom) // Pick up the new one
                pickUp(slotTo) // Put the new one into armor slot
                postDelay(delay, TimeUnit.TICKS)
            }
        } else {
            inventoryTask {
                pickUp(slotFrom) // Pick up the new one
                pickUp(slotTo) // Put the new one into armor slot
                pickUp(slotFrom) // Put the old one into the empty slot
                postDelay(delay, TimeUnit.TICKS)
            }
        }
    }

    private fun SafeClientEvent.moveFromInventory(slotFrom: Slot, slotTo: Slot): InventoryTask {
        return when {
            !slotTo.hasStack -> {
                inventoryTask {
                    quickMove(slotFrom) // Move the new one into armor slot)
                    postDelay(delay, TimeUnit.TICKS)
                }
            }
            player.inventorySlots.hasEmpty() -> {
                inventoryTask {
                    quickMove(slotTo) // Move out the old one
                    quickMove(slotFrom) // Put the old one into the empty slot
                    postDelay(delay, TimeUnit.TICKS)
                }
            }
            else -> {
                inventoryTask {
                    pickUp(slotFrom) // Pick up the new one
                    pickUp(slotTo) // Put the new one into armor slot
                    postDelay(delay, TimeUnit.TICKS)
                }
            }
        }
    }
}