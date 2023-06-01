package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.extension.fastCeil
import dev.luna5ama.trollhack.util.inventory.InventoryTask
import dev.luna5ama.trollhack.util.inventory.confirmedOrTrue
import dev.luna5ama.trollhack.util.inventory.inventoryTask
import dev.luna5ama.trollhack.util.inventory.operation.moveTo
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
            if (!lastTask.confirmedOrTrue) return@safeParallelListener

            val sourceSlots = if (prioritizeCraftingSlot) {
                player.craftingSlots + player.storageSlots
            } else {
                player.storageSlots + player.craftingSlots
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