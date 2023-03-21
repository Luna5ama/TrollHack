package me.luna.trollhack.module.modules.player

import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.event.events.RunGameLoopEvent
import me.luna.trollhack.event.safeConcurrentListener
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.collections.IntBitSet
import me.luna.trollhack.util.inventory.InventoryTask
import me.luna.trollhack.util.inventory.executedOrTrue
import me.luna.trollhack.util.inventory.inventoryTask
import me.luna.trollhack.util.inventory.operation.pickUp
import me.luna.trollhack.util.inventory.slot.getMaxCompatibleStack
import me.luna.trollhack.util.inventory.slot.inventorySlots
import me.luna.trollhack.util.text.NoSpamMessage
import me.luna.trollhack.util.threads.runSafe
import net.minecraft.init.Items
import net.minecraft.inventory.Slot
import net.minecraft.item.Item

internal object InventorySorter : Module(
    name = "InventorySorter",
    category = Category.PLAYER,
    description = "Sort out items in inventory",
    modulePriority = 20
) {
    private val clickDelay by setting("Click Delay", 10, 0..1000, 1)
    private val postDelay by setting("Post Delay", 50, 0..1000, 1)

    private val checkSet = IntBitSet()
    private var itemArray: Array<Item>? = null
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

    private fun SafeClientEvent.runSorting(itemArray: Array<Item>) {
        val slots = mutableListOf<Slot>()
        player.inventorySlots.filterNotTo(slots) { checkSet.contains(it.slotNumber - 9) }

        for (index in 35 downTo 0) {
            lastIndex = index
            if (checkSet.contains(index)) continue

            val targetItem = itemArray[index]
            if (targetItem == Items.AIR) continue
            val slotTo = slots[index]
            val stackTo = slotTo.stack

            val slot = if (stackTo.item != targetItem) {
                slots.find {
                    it.slotNumber != slotTo.slotNumber && it.stack.item == targetItem
                }
            } else {
                slots.getMaxCompatibleStack(slotTo, targetItem)
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
