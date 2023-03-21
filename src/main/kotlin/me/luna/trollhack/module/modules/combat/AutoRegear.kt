package me.luna.trollhack.module.modules.combat

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.event.events.RunGameLoopEvent
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.gui.hudgui.elements.client.Notification
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.module.modules.player.InventorySorter
import me.luna.trollhack.module.modules.player.Kit
import me.luna.trollhack.util.TickTimer
import me.luna.trollhack.util.inventory.InventoryTask
import me.luna.trollhack.util.inventory.executedOrTrue
import me.luna.trollhack.util.inventory.inventoryTask
import me.luna.trollhack.util.inventory.operation.pickUp
import me.luna.trollhack.util.inventory.operation.swapWith
import me.luna.trollhack.util.inventory.slot.*
import net.minecraft.inventory.Container
import net.minecraft.inventory.ContainerShulkerBox
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemShulkerBox

internal object AutoRegear : Module(
    name = "AutoRegear",
    description = "Automatically regear using container",
    category = Category.COMBAT
) {
    private val shulkearBoxOnly by setting("Shulker Box Only", true)
    private val takeArmor by setting("Take Armor", true)
    private val clickDelayMs by setting("Click Delay ms", 10, 0..1000, 1)
    private val postDelayMs by setting("Post Delay ms", 50, 0..1000, 1)
    private val moveTimeoutMs by setting("Move Timeout ms", 100, 0..1000, 1)

    private val armorTimer = TickTimer()
    private val timeoutTimer = TickTimer()
    private var lastContainer: Container? = null
    private var lastTask: InventoryTask? = null
    private val moveTimeMap = Int2LongOpenHashMap().apply {
        defaultReturnValue(Long.MIN_VALUE)
    }

    override fun getHudInfo(): String {
        return Kit.kitName
    }

    init {
        onDisable {
            lastContainer = null
            lastTask?.cancel()
            lastTask = null
            moveTimeMap.clear()
        }

        safeListener<RunGameLoopEvent.Tick> {
            if (!lastTask.executedOrTrue) return@safeListener

            val openContainer = player.openContainer
            if (openContainer === player.inventoryContainer
                && (!shulkearBoxOnly || openContainer !is ContainerShulkerBox)
            ) {
                lastTask?.cancel()
                return@safeListener
            }

            if (openContainer !== lastContainer) {
                moveTimeMap.clear()
                timeoutTimer.time = Long.MAX_VALUE
                lastContainer = openContainer
            } else if (timeoutTimer.tick(2000)) {
                return@safeListener
            }

            val itemArray = Kit.getKitItemArray() ?: run {
                Notification.send(InventorySorter, "No kit named ${Kit.kitName} was not found!")
                return@safeListener
            }

            if (takeArmor(openContainer)) return@safeListener
            if (doRegear(openContainer, itemArray)) return@safeListener
        }
    }

    private fun SafeClientEvent.takeArmor(
        openContainer: Container
    ): Boolean {
        if (!takeArmor) return false

//        AutoArmor.enable()

        val windowID = openContainer.windowId
        val currentTime = System.currentTimeMillis()
        val containerSlots = openContainer.getContainerSlots().filter { currentTime > moveTimeMap[it.slotNumber] }
        val playerInventory = player.allSlots
        val tempHotbarSlot = player.hotbarSlots.firstEmpty()
            ?: player.hotbarSlots.find {
                val item = it.stack.item
                item !is ItemShulkerBox && item !is ItemArmor
            } ?: return false

        for (slotFrom in containerSlots) {
            val stack = slotFrom.stack
            val item = stack.item
            if (item !is ItemArmor) continue

            if (playerInventory.any {
                    val playetItem = it.stack.item
                    playetItem is ItemArmor && playetItem.armorType == item.armorType
                }) continue

            if (!armorTimer.tickAndReset(100L)) {
                timeoutTimer.time = Long.MAX_VALUE
                return true
            }

            lastTask = inventoryTask {
                swapWith(windowID, slotFrom, tempHotbarSlot)

                delay(clickDelayMs)
                postDelay(postDelayMs)
                runInGui()
            }

            moveTimeMap[slotFrom.slotNumber] = currentTime + moveTimeoutMs
            timeoutTimer.time = Long.MAX_VALUE

            return true
        }

        return false
    }

    private fun doRegear(
        openContainer: Container,
        itemArray: Array<Item>
    ): Boolean {
        val windowID = openContainer.windowId
        val currentTime = System.currentTimeMillis()
        val containerSlots = mutableListOf<Slot>()
        openContainer.getContainerSlots().filterTo(containerSlots) { currentTime > moveTimeMap[it.slotNumber] }

        val playerSlot = openContainer.getPlayerSlots()

        for (index in playerSlot.indices) {
            val targetItem = itemArray[index]
            if (targetItem is ItemShulkerBox) continue

            val slotTo = playerSlot[index]
            if (index in playerSlot.size - 9 until playerSlot.size
                && slotTo.stack.item is ItemArmor
            ) continue

            val slotFrom = containerSlots.getMaxCompatibleStack(slotTo, targetItem) ?: continue

            lastTask = inventoryTask {
                pickUp(windowID, slotFrom)
                pickUp(windowID, slotTo)
                pickUp(windowID) {
                    if (player.inventory.getCurrentItem().isEmpty) null else slotFrom
                }

                delay(clickDelayMs)
                postDelay(postDelayMs)
                runInGui()
            }

            moveTimeMap[slotFrom.slotNumber] = currentTime + moveTimeoutMs
            timeoutTimer.time = Long.MAX_VALUE
            containerSlots.remove(slotFrom)

            return true
        }

        if (timeoutTimer.time == Long.MAX_VALUE) {
            timeoutTimer.reset()
        }

        return false
    }
}
