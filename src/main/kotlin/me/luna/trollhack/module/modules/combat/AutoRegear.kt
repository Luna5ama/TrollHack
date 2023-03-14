package me.luna.trollhack.module.modules.combat

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
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
import me.luna.trollhack.util.inventory.slot.getCompatibleStack
import me.luna.trollhack.util.inventory.slot.getContainerSlots
import me.luna.trollhack.util.inventory.slot.getPlayerSlots
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.item.Item

internal object AutoRegear : Module(
    name = "AutoRegear",
    description = "Automatically regear using container",
    category = Category.COMBAT
) {
    private val clickDelayMs by setting("Click Delay ms", 10, 0..1000, 1)
    private val postDelayMs by setting("Post Delay ms", 50, 0..1000, 1)
    private val moveTimeoutMs by setting("Move Timeout ms", 100, 0..1000, 1)

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
            if (openContainer === player.inventoryContainer) {
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

            doRegear(openContainer, itemArray)
        }
    }

    private fun doRegear(
        openContainer: Container,
        itemArray: Array<Item>
    ) {
        val currentTime = System.currentTimeMillis()
        val containerSlots = mutableListOf<Slot>()
        openContainer.getContainerSlots().filterTo(containerSlots) { currentTime > moveTimeMap[it.slotNumber] }

        val playerSlot = openContainer.getPlayerSlots()

        for (index in playerSlot.indices) {
            val targetItem = itemArray[index]
            val slotTo = playerSlot[index]
            val slotFrom = containerSlots.getCompatibleStack(slotTo, targetItem) ?: continue

            lastTask = inventoryTask {
                val windowID = openContainer.windowId
                pickUp(windowID, slotFrom)
                pickUp(windowID, slotTo)
                pickUp(windowID) {
                    if (player.inventory.getCurrentItem().isEmpty) null else slotFrom
                }

                delay(clickDelayMs)
                postDelay(postDelayMs)
                runInGui()
            }

            moveTimeMap[slotFrom.slotIndex] = currentTime + moveTimeoutMs
            containerSlots.remove(slotFrom)

            timeoutTimer.time = Long.MAX_VALUE

            if (clickDelayMs != 0 || postDelayMs != 0) {
                return
            }
        }

        if (timeoutTimer.time == Long.MAX_VALUE) {
            timeoutTimer.reset()
        }
    }
}
