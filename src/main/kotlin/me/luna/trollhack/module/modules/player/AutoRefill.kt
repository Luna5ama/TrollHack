package me.luna.trollhack.module.modules.player

import me.luna.trollhack.event.events.TickEvent
import me.luna.trollhack.event.safeParallelListener
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.extension.fastCeil
import me.luna.trollhack.util.inventory.InventoryTask
import me.luna.trollhack.util.inventory.confirmedOrTrue
import me.luna.trollhack.util.inventory.inventoryTask
import me.luna.trollhack.util.inventory.operation.moveTo
import me.luna.trollhack.util.inventory.slot.*

internal object AutoRefill : Module(
    name = "AutoRefill",
    category = Category.PLAYER,
    description = "Automatically refills stackable items in your hotbar",
    modulePriority = 15
) {
    private val prioritizeCraftingSlot by setting("Prioritize Crafting Slot", true)
    private val refillThreshold by setting("Refill Threshold", 16, 1..63, 1,)
    private val delayMs by setting("Delay ms", 50, 0..1000, 5)

    private var lastTask: InventoryTask? = null

    init {
        safeParallelListener<TickEvent.Post> {
            if (!lastTask.confirmedOrTrue) return@safeParallelListener

            val sourceSlots = if (prioritizeCraftingSlot) {
                player.craftingSlots + player.inventorySlots
            } else {
                player.inventorySlots + player.craftingSlots
            }

            val targetSlots = player.hotbarSlots + player.offhandSlot

            for (slotTo in targetSlots) {
                val stack = slotTo.stack
                if (stack.isEmpty) continue
                if (!stack.isStackable) continue
                if (stack.count >= (stack.maxStackSize / 64.0f * refillThreshold).fastCeil()) continue
                if (AutoEject.ejectMap.value.containsKey(stack.item.registryName.toString())) continue

                val slotFrom = sourceSlots.getMaxCompatibleStack(slotTo) ?: continue
                inventoryTask {
                    moveTo(slotFrom, slotTo)
                    runInGui()
                    delay(0)
                    postDelay(delayMs)
                }
                break
            }
        }
    }
}