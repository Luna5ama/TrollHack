package dev.luna5ama.trollhack.module.modules.player

import dev.fastmc.common.ceilToInt
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.inventory.InventoryTask
import dev.luna5ama.trollhack.util.inventory.executedOrTrue
import dev.luna5ama.trollhack.util.inventory.inventoryTask
import dev.luna5ama.trollhack.util.inventory.operation.moveTo
import dev.luna5ama.trollhack.util.inventory.operation.quickMove
import dev.luna5ama.trollhack.util.inventory.slot.*

internal object HotbarRefill : Module(
    name = "Hotbar Refill",
    category = Category.PLAYER,
    description = "Automatically refills stackable items in your hotbar",
    modulePriority = 15
) {
    private val prioritizeCraftingSlot by setting("Prioritize Crafting Slot", true)
    private val refillThreshold by setting("Refill Threshold", 16, 1..63, 1)
    private val delayMs by setting("Delay ms", 50, 0..1000, 5)

    private var lastTask: InventoryTask? = null

    init {
        safeParallelListener<TickEvent.Post> {
            if (!lastTask.executedOrTrue) return@safeParallelListener

            val sourceSlots = if (prioritizeCraftingSlot) {
                player.craftingSlots + player.storageSlots
            } else {
                player.storageSlots + player.craftingSlots
            }

            val targetSlots = player.hotbarSlots + player.offhandSlot

            for (slotTo in targetSlots.asReversed()) {
                val stack = slotTo.stack
                if (stack.isEmpty) continue
                if (!stack.isStackable) continue
                if (stack.count >= (stack.maxStackSize / 64.0f * refillThreshold).ceilToInt()) continue
                if (AutoEject.ejectMap.value.containsKey(stack.item.registryName.toString())) continue

                val slotFrom = sourceSlots.findFirstCompatibleStack(slotTo) ?: continue
                lastTask = if (slotTo is HotbarSlot) {
                    inventoryTask {
                        quickMove(slotFrom)
                        runInGui()
                        delay(0)
                        postDelay(delayMs)
                    }
                } else {
                    inventoryTask {
                        moveTo(slotFrom, slotTo)
                        runInGui()
                        delay(0)
                        postDelay(delayMs)
                    }
                }
                break
            }
        }
    }
}