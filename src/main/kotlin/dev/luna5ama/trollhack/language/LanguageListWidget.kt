package dev.luna5ama.trollhack.language

import dev.luna5ama.trollhack.interfaces.ILanguageOptionsScreen
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.ObjectSelectionList
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.options.LanguageSelectScreen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import kotlin.math.min

class LanguageListWidget(
    val client: Minecraft,
    val screen: LanguageSelectScreen,
    width: Int,
    height: Int,
    private val title: Component
) : ObjectSelectionList<LanguageEntry>(client, width, height - 83 - 16, 32 + 16, 24) {

    init {
        centerListVertically = false
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        return keyPressed(event.key(), event.scancode(), event.modifiers())
    }

    fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val selectedEntry = this.selected
        if (selectedEntry == null) return super.keyPressed(KeyEvent(keyCode, scanCode, modifiers))

        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER) {
            selectedEntry.toggle()
            this.setFocused(null)
            (screen as ILanguageOptionsScreen).languagereload_focusEntry(selectedEntry)
            return true
        }

        if (client.hasShiftDown()) {
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                selectedEntry.moveDown()
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                selectedEntry.moveUp()
                return true
            }
        }

        return super.keyPressed(KeyEvent(keyCode, scanCode, modifiers))
    }

    override fun getEntryAtPosition(x: Double, y: Double): LanguageEntry? {
        var entry = super.getEntryAtPosition(x, y)
        return if (entry != null && this.scrollable() && x >= this.scrollBarX()) null else entry
    }

    override fun extractSelection(context: GuiGraphicsExtractor, entry: LanguageEntry, y: Int) {
        if (this.scrollable()) {
            var x1 = this.rowLeft - 2
            var x2 = this.scrollBarX()
            var y1 = entry.y - 2
            var y2 = entry.y + entry.height + 2
            context.fill(x1, y1, x2, y2, -8355712)
            context.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, -16777216)
        } else {
            super.extractSelection(context, entry, y)
        }
    }

    fun getHoveredSelectionRight(): Int {
        return if (this.scrollable()) this.scrollBarX() else this.rowRight - 2
    }

    val rowHeight get() = 24

    override fun getRowWidth(): Int {
        return width
    }

    override fun scrollBarX(): Int {
        return this.right - 6
    }
}
