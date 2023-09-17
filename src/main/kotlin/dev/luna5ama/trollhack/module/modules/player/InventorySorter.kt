package dev.luna5ama.trollhack.module.modules.player

import dev.fastmc.common.collection.DynamicBitSet
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.safeConcurrentListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.inventory.InventoryTask
import dev.luna5ama.trollhack.util.inventory.executedOrTrue
import dev.luna5ama.trollhack.util.inventory.inventoryTask
import dev.luna5ama.trollhack.util.inventory.operation.pickUp
import dev.luna5ama.trollhack.util.inventory.slot.findFirstCompatibleStack
import dev.luna5ama.trollhack.util.inventory.slot.findMaxCompatibleStack
import dev.luna5ama.trollhack.util.inventory.slot.inventorySlots
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.runSafe
import net.minecraft.init.Items
import net.minecraft.inventory.Slot

internal object InventorySorter : Module(
    name = "Inventory Sorter",
    category = Category.PLAYER,
    description = "Sort out items in inventory",
    modulePriority = 20
) {
    private val clickDelay by setting("Click Delay", 10, 0..1000, 1)
    private val postDelay by setting("Post Delay", 50, 0..1000, 1)

    private val checkSet = DynamicBitSet()
    private var itemArray: Array<Kit.ItemEntry>? = null
    private var lastTask: InventoryTask? = null
    private var lastIndex = 35

    override fun getHudInfo(): String {
        return Kit.kitName
    }

    init {
        onEnable {
            runSafe {
                itemArray = Kit.getKitItemArray() ?: run {
                    NoSpamMessage.sendError(InventorySorter, "No kit named ${Kit.kitName} was not found!")
                    disable()
                    return@onEnable
                }
            } ?: disable()
        }

        onDisable {
            itemArray = null
            lastTask?.cancel()
            lastTask = null
            lastIndex = 35
            checkSet.clear()
        }

        safeConcurrentListener<RunGameLoopEvent.Tick> {
            val itemArray = itemArray
            if (itemArray == null) {
                disable()
                return@safeConcurrentListener
            }

            if (!lastTask.executedOrTrue) return@safeConcurrentListener
            if (lastIndex == 0) {
                NoSpamMessage.sendMessage(InventorySorter, "Finished sorting!")
                disable()
                return@safeConcurrentListener
            }

            runSorting(itemArray)
        }
    }

    private fun SafeClientEvent.runSorting(itemArray: Array<Kit.ItemEntry>) {
        val slots = mutableListOf<Slot>()
        player.inventorySlots.filterNotTo(slots) { checkSet.contains(it.slotNumber - 9) }

        for (index in 35 downTo 0) {
            lastIndex = index
            if (checkSet.contains(index)) continue

            val targetItem = itemArray[index]
            if (targetItem.item == Items.AIR) continue
            val slotTo = slots[index]
            val stackTo = slotTo.stack

            val slot = if (!targetItem.equals(stackTo)) {
                slots.find {
                    it.slotNumber != slotTo.slotNumber && targetItem.equals(it.stack)
                } ?: slots.find {
                    it.slotNumber != slotTo.slotNumber && targetItem.item == it.stack.item
                }
            } else {
                slots.findFirstCompatibleStack(slotTo, targetItem)
            }

            if (slot == null) {
                slots.remove(slotTo)
                checkSet.add(index)
                continue
            } else {
                lastTask = moveItem(slot, slotTo)
                return
            }
        }
    }

    private fun moveItem(slotFrom: Slot, slotTo: Slot): InventoryTask {
        return inventoryTask {
            pickUp(slotFrom)
            pickUp(slotTo)
            pickUp {
                if (player.inventory.getCurrentItem().isEmpty) null else slotFrom
            }
            runInGui()
            delay(clickDelay)
            postDelay(postDelay)
        }
    }

}