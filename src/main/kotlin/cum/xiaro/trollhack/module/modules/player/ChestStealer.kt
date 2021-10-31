package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.Wrapper
import cum.xiaro.trollhack.util.inventory.InventoryTask
import cum.xiaro.trollhack.util.inventory.executedOrTrue
import cum.xiaro.trollhack.util.inventory.inventoryTask
import cum.xiaro.trollhack.util.inventory.operation.moveTo
import cum.xiaro.trollhack.util.inventory.operation.quickMove
import cum.xiaro.trollhack.util.inventory.operation.throwAll
import cum.xiaro.trollhack.util.inventory.slot.firstByStack
import cum.xiaro.trollhack.util.inventory.slot.firstEmpty
import cum.xiaro.trollhack.util.inventory.slot.getSlots
import cum.xiaro.trollhack.util.threads.runSafe
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiEnchantment
import net.minecraft.client.gui.GuiMerchant
import net.minecraft.client.gui.GuiRepair
import net.minecraft.client.gui.inventory.*
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemShulkerBox

internal object ChestStealer : Module(
    name = "ChestStealer",
    category = Category.PLAYER,
    description = "Automatically steal or store items from containers"
) {
    val mode by setting("Mode", Mode.TOGGLE)
    private val movingMode by setting("Moving Mode", MovingMode.QUICK_MOVE)
    private val ignoreEjectItem by setting("Ignores Eject Item", false, description = "Ignore AutoEject items in InventoryManager")
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
        safeListener<TickEvent.Pre> {
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
            val size = getContainerSlotSize()
            val rangeStart = if (containerMode == ContainerMode.STEAL) size else 0
            val slotTo = player.openContainer.getSlots(rangeStart until size + containerMode.offset).firstEmpty()
                ?: return false
            val windowID = player.openContainer.windowId

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

        return container.getSlots(0 until getContainerSlotSize()).firstByStack {
            !it.isEmpty
                && (!shulkersOnly || it.item is ItemShulkerBox)
                && (!ignoreEjectItem || !InventoryManager.ejectList.contains(it.item.registryName.toString()))
        }
    }

    private fun SafeClientEvent.getStoringSlot(): Slot? {
        val container = player.openContainer
        val size = getContainerSlotSize()

        return container.getSlots(size until size + 36).firstByStack {
            !it.isEmpty
                && (!shulkersOnly || it.item is ItemShulkerBox)
        }
    }

    private fun SafeClientEvent.getContainerSlotSize(): Int {
        if (mc.currentScreen !is GuiContainer) return 0
        return player.openContainer.inventorySlots.size - 36
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
