package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeConcurrentListener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.inventory.InventoryTask
import dev.luna5ama.trollhack.util.inventory.executedOrTrue
import dev.luna5ama.trollhack.util.inventory.inventoryTask
import dev.luna5ama.trollhack.util.inventory.operation.moveTo
import dev.luna5ama.trollhack.util.inventory.operation.quickMove
import dev.luna5ama.trollhack.util.inventory.operation.throwAll
import dev.luna5ama.trollhack.util.inventory.slot.*
import dev.luna5ama.trollhack.util.threads.runSafe
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiEnchantment
import net.minecraft.client.gui.GuiMerchant
import net.minecraft.client.gui.GuiRepair
import net.minecraft.client.gui.inventory.GuiBeacon
import net.minecraft.client.gui.inventory.GuiContainerCreative
import net.minecraft.client.gui.inventory.GuiCrafting
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemShulkerBox

internal object ChestStealer : Module(
    name = "Chest Stealer",
    category = Category.PLAYER,
    description = "Automatically steal or store items from containers"
) {
    val mode by setting("Mode", Mode.TOGGLE)
    private val movingMode by setting("Moving Mode", MovingMode.QUICK_MOVE)
    private val ignoreEjectItem by setting(
        "Ignores Eject Item",
        false,
        description = "Ignore AutoEject items in AutoEject"
    )
    private val delay by setting("Delay", 250, 0..1000, 25, description = "Move stack delay in ms")
    private val shulkersOnly by setting("Shulkers Only", false, description = "Only move shulker boxes")

    enum class Mode {
        ALWAYS, TOGGLE, MANUAL
    }

    private enum class MovingMode {
        QUICK_MOVE, PICKUP, THROW
    }

    private enum class ContainerMode(val offset: Int) {
        STEAL(36),
        STORE(0)
    }

    var stealing = false
    var storing = false

    private var lastTask: InventoryTask? = null

    init {
        safeConcurrentListener<RunGameLoopEvent.Tick> {
            val flag = isContainerOpen()

            stealing = flag && (stealing || mode == Mode.ALWAYS) && stealOrStore(getStealingSlot(), ContainerMode.STEAL)
            storing = flag && storing && stealOrStore(getStoringSlot(), ContainerMode.STORE)
        }
    }

    private fun SafeClientEvent.canSteal(): Boolean {
        return getStealingSlot() != null
    }

    private fun SafeClientEvent.canStore(): Boolean {
        return getStoringSlot() != null
    }

    private fun SafeClientEvent.isContainerOpen(): Boolean {
        return player.openContainer != null
            && isValidGui()
    }

    fun isValidGui(): Boolean {
        return mc.currentScreen !is GuiEnchantment
            && mc.currentScreen !is GuiMerchant
            && mc.currentScreen !is GuiRepair
            && mc.currentScreen !is GuiBeacon
            && mc.currentScreen !is GuiCrafting
            && mc.currentScreen !is GuiContainerCreative
            && mc.currentScreen !is GuiInventory
    }

    private fun SafeClientEvent.stealOrStore(slot: Slot?, containerMode: ContainerMode): Boolean {
        if (slot == null) return false

        if (lastTask.executedOrTrue) {
            val openContainer = player.openContainer
            val size = openContainer.getContainerSlotSize()
            val rangeStart = if (containerMode == ContainerMode.STEAL) size else 0
            val slotTo = openContainer.getSlots(rangeStart until size + containerMode.offset).firstEmpty()
                ?: return false
            val windowID = openContainer.windowId

            lastTask = inventoryTask {
                when (movingMode) {
                    MovingMode.QUICK_MOVE -> {
                        quickMove(windowID, slot)
                    }
                    MovingMode.PICKUP -> {
                        moveTo(windowID, slot, slotTo)
                    }
                    MovingMode.THROW -> {
                        throwAll(windowID, slot)
                    }
                }
                delay(0L)
                postDelay(delay)
            }
        }

        return true
    }

    private fun SafeClientEvent.getStealingSlot(): Slot? {
        val container = player.openContainer

        return container.getContainerSlots().firstByStack {
            !it.isEmpty
                && (!shulkersOnly || it.item is ItemShulkerBox)
                && (!ignoreEjectItem || !AutoEject.ejectMap.value.containsKey(it.item.registryName.toString()))
        }
    }

    private fun SafeClientEvent.getStoringSlot(): Slot? {
        val container = player.openContainer
        val size = container.getContainerSlotSize()

        return container.getSlots(size until size + 36).firstByStack {
            !it.isEmpty
                && (!shulkersOnly || it.item is ItemShulkerBox)
        }
    }

    class StoreButton : GuiButton(420420, 0, 0, 50, 20, "Store") {
        override fun mousePressed(mc: Minecraft, mouseX: Int, mouseY: Int): Boolean {
            val pressed = super.mousePressed(mc, mouseX, mouseY)
            if (pressed) storing = !storing
            return pressed
        }

        override fun mouseReleased(mouseX: Int, mouseY: Int) {
            if (mode == Mode.MANUAL) {
                storing = false
                playPressSound(Wrapper.minecraft.soundHandler)
            }
            super.mouseReleased(mouseX, mouseY)
        }

        fun update(left: Int, top: Int, size: Int) {
            runSafe {
                if (isEnabled && isContainerOpen()) {
                    x = left + size + 2
                    y = top + 24
                    enabled = !stealing && canStore()
                    visible = true
                    displayString = if (storing) "Stop" else "Store"
                } else {
                    visible = false
                }
            }
        }
    }

    class StealButton : GuiButton(696969, 0, 0, 50, 20, "Steal") {
        override fun mousePressed(mc: Minecraft, mouseX: Int, mouseY: Int): Boolean {
            val pressed = super.mousePressed(mc, mouseX, mouseY)
            if (pressed) stealing = !stealing
            return pressed
        }

        override fun mouseReleased(mouseX: Int, mouseY: Int) {
            if (mode == Mode.MANUAL) {
                stealing = false
                playPressSound(Wrapper.minecraft.soundHandler)
            }
            super.mouseReleased(mouseX, mouseY)
        }

        fun update(left: Int, top: Int, size: Int) {
            runSafe {
                if (isEnabled && isContainerOpen()) {
                    x = left + size + 2
                    y = top + 2
                    enabled = !storing && canSteal()
                    visible = true
                    displayString = if (stealing) "Stop" else "Steal"
                } else {
                    visible = false
                }
            }
        }
    }
}