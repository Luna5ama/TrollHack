package dev.luna5ama.trollhack.gui

import dev.luna5ama.trollhack.gui.legacy.LegacyClickGui
import dev.luna5ama.trollhack.modules.impl.client.ClickGui
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

object NullClickGui : Screen(Component.literal("TrollHack ClickGui")) {
    fun open() {
        LegacyClickGui.open()
    }

    fun reloadPanel() {
        LegacyClickGui.reloadPanel()
    }

    override fun isPauseScreen(): Boolean {
        return ClickGui.pauseGame
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        LegacyClickGui.render(context, mouseX, mouseY, delta)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        LegacyClickGui.mouseMoved(mouseX, mouseY)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        return LegacyClickGui.mouseClicked(event.x(), event.y(), event.button())
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        return LegacyClickGui.mouseReleased(event.x(), event.y(), event.button())
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return LegacyClickGui.mouseScrolled(mouseX, mouseY, verticalAmount)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        return LegacyClickGui.keyPressed(event.key(), event.scancode())
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        return LegacyClickGui.charTyped(event.codepoint().toChar())
    }

    override fun removed() {
        if (ClickGui.isEnabled && minecraft?.screen !== this) {
            ClickGui.disable()
        }
    }
}
