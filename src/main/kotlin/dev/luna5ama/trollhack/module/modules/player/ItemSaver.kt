package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.inventory.InventoryTask
import dev.luna5ama.trollhack.util.inventory.confirmedOrTrue
import dev.luna5ama.trollhack.util.inventory.id
import dev.luna5ama.trollhack.util.inventory.inventoryTask
import dev.luna5ama.trollhack.util.inventory.operation.swapWith
import dev.luna5ama.trollhack.util.inventory.slot.*
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

internal object ItemSaver : Module(
    name = "Item Saver",
    category = Category.PLAYER,
    description = "Saves your items from breaking",
    modulePriority = 20
) {
    private val duraThreshold by setting("Durability Threshold", 5, 1..50, 1)
    private val delayMs by setting("Delay ms", 50, 0..1000, 5)

    private var lastTask: InventoryTask? = null

    init {
        safeParallelListener<TickEvent.Post> {
            if (!lastTask.confirmedOrTrue) return@safeParallelListener
            if (!checkDamage(player.heldItemMainhand)) return@safeParallelListener

            val currentSlot = player.currentHotbarSlot
            val itemStack = player.heldItemMainhand
            val undamagedItem = getUndamagedItem(itemStack.item.id)
            val emptySlot = player.inventorySlots.firstEmpty()

            when {
                undamagedItem != null -> {
                    inventoryTask {
                        swapWith(undamagedItem, currentSlot)
                        runInGui()
                        delay(0)
                        postDelay(delayMs)
                    }
                }
                emptySlot != null -> {
                    inventoryTask {
                        swapWith(emptySlot, currentSlot)
                        runInGui()
                        delay(0)
                        postDelay(delayMs)
                    }
                }
                else -> {
                    player.dropItem(false)
                }
            }
        }
    }

    private fun checkDamage(itemStack: ItemStack): Boolean {
        return (itemStack.isItemStackDamageable
            && itemStack.itemDamage > itemStack.maxDamage * (1.0f - duraThreshold / 100.0f))
    }

    /**
     * Finds undamaged item with given ID in inventory, and return its slot
     *
     * @return Full inventory slot if undamaged item found, else return null
     */
    private fun SafeClientEvent.getUndamagedItem(itemID: Int): Slot? {
        return player.storageSlots.firstID(itemID) {
            !checkDamage(it)
        }
    }
}