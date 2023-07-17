package dev.luna5ama.trollhack.gui.hudgui.elements.player

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.graphics.RenderUtils2D
import dev.luna5ama.trollhack.gui.hudgui.HudElement
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.inventory.slot.storageSlots
import dev.luna5ama.trollhack.util.threads.runSafe

internal object InventoryViewer : HudElement(
    name = "Inventory Viewer",
    category = Category.PLAYER,
    description = "Items in Inventory"
) {
    private val border by setting("Border", true)
    private val background by setting("Background", true)

    override val hudWidth = 162.0f
    override val hudHeight = 54.0f

    override fun renderHud() {
        super.renderHud()
        runSafe {
            drawFrame()
            drawItems()
        }
    }

    private fun drawFrame() {
        if (background) {
            RenderUtils2D.drawRectFilled(0.0f, 0.0f, 162.0f, 54.0f, color = GuiSetting.backGround)
        }
        if (border) {
            RenderUtils2D.drawRectOutline(0.0f, 0.0f, 162.0f, 54.0f, lineWidth = 2.0f, color = GuiSetting.primary)
        }
    }

    private fun SafeClientEvent.drawItems() {
        for ((index, slot) in player.storageSlots.withIndex()) {
            val itemStack = slot.stack
            if (itemStack.isEmpty) continue

            val slotX = index % 9 * 18 + 1
            val slotY = index / 9 * 18 + 1

            RenderUtils2D.drawItem(itemStack, slotX, slotY)
        }
    }

}