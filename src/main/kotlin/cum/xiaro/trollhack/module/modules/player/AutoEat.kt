package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.process.PauseProcess.pauseBaritone
import cum.xiaro.trollhack.process.PauseProcess.unpauseBaritone
import cum.xiaro.trollhack.util.combat.CombatUtils.scaledHealth
import cum.xiaro.trollhack.util.inventory.InventoryTask
import cum.xiaro.trollhack.util.inventory.confirmedOrTrue
import cum.xiaro.trollhack.util.inventory.inventoryTask
import cum.xiaro.trollhack.util.inventory.operation.swapToItem
import cum.xiaro.trollhack.util.inventory.operation.swapToSlot
import cum.xiaro.trollhack.util.inventory.operation.swapWith
import cum.xiaro.trollhack.util.inventory.slot.anyHotbarSlot
import cum.xiaro.trollhack.util.inventory.slot.firstItem
import cum.xiaro.trollhack.util.inventory.slot.storageSlots
import cum.xiaro.trollhack.util.threads.runSafe
import net.minecraft.client.settings.KeyBinding
import net.minecraft.init.Items
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemTool
import net.minecraft.util.EnumHand

internal object AutoEat : Module(
    name = "AutoEat",
    description = "Automatically eat when hungry",
    category = Category.PLAYER
) {
    private val belowHunger by setting("Below Hunger", 15, 1..20, 1)
    private val belowHealth by setting("Below Health", 10, 1..20, 1)
    private val eatBadFood by setting("Eat Bad Food", false)
    private val pauseBaritone by setting("Pause Baritone", true)

    private var lastSlot = -1
    private var eating = false
    private var lastTask: InventoryTask? = null

    override fun isActive(): Boolean {
        return isEnabled && eating
    }

    init {
        onDisable {
            stopEating()
            swapBack()
        }

        safeListener<TickEvent.Pre> {
            if (!lastTask.confirmedOrTrue) return@safeListener

            if (!player.isEntityAlive) {
                if (eating) stopEating()
                return@safeListener
            }

            val hand = when {
                !shouldEat() -> {
                    null // Null = stop eating
                }
                isValid(player.heldItemOffhand) -> {
                    EnumHand.OFF_HAND
                }
                isValid(player.heldItemMainhand) -> {
                    EnumHand.MAIN_HAND
                }
                swapToFood() -> { // If we found valid food and moved
                    // Set eating and pause then return and wait until next tick
                    startEating()
                    return@safeListener
                }
                else -> {
                    null // If we can't find any valid food then stop eating
                }
            }

            if (hand != null) {
                eat(hand)
            } else {
                // Stop eating first and swap back in the next tick
                if (eating) {
                    stopEating()
                } else {
                    swapBack()
                }
            }
        }
    }

    private fun SafeClientEvent.shouldEat() =
        player.foodStats.foodLevel < belowHunger
            || player.scaledHealth < belowHealth

    private fun SafeClientEvent.eat(hand: EnumHand) {
        if (!eating || !player.isHandActive || player.activeHand != hand) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, true)

            // Vanilla Minecraft prioritize offhand so we need to force it using the specific hand
            playerController.processRightClick(player, world, hand)
        }

        startEating()
    }

    private fun startEating() {
        if (pauseBaritone) pauseBaritone()
        eating = true
    }

    private fun stopEating() {
        unpauseBaritone()

        runSafe {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
        }

        eating = false
    }

    private fun swapBack() {
        val slot = lastSlot
        if (slot == -1) return

        lastSlot = -1
        runSafe {
            swapToSlot(slot)
        }
    }

    /**
     * @return `true` if food found and moved
     */
    private fun SafeClientEvent.swapToFood(): Boolean {
        lastSlot = player.inventory.currentItem
        val hasFoodInSlots = swapToItem<ItemFood> { isValid(it) }

        return if (hasFoodInSlots) {
            true
        } else {
            lastSlot = -1
            moveFoodToHotbar()
        }
    }

    /**
     * @return `true` if food found and moved
     */
    private fun SafeClientEvent.moveFoodToHotbar(): Boolean {
        val slotFrom = player.storageSlots.firstItem<ItemFood, Slot> {
            isValid(it)
        } ?: return false

        val slotTo = player.anyHotbarSlot {
            val item = it.item
            item !is ItemTool && item !is ItemBlock
        }

        lastTask = inventoryTask {
            swapWith(slotFrom, slotTo)
        }

        return true
    }

    private fun SafeClientEvent.isValid(itemStack: ItemStack): Boolean {
        val item = itemStack.item

        return item is ItemFood
            && item != Items.CHORUS_FRUIT
            && (eatBadFood || !isBadFood(itemStack, item))
            && player.canEat(item == Items.GOLDEN_APPLE)
    }

    private fun isBadFood(itemStack: ItemStack, item: ItemFood) =
        item == Items.ROTTEN_FLESH
            || item == Items.SPIDER_EYE
            || item == Items.POISONOUS_POTATO
            || item == Items.FISH && (itemStack.metadata == 3 || itemStack.metadata == 2) // Puffer fish, Clown fish
}