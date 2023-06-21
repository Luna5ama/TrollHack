package dev.luna5ama.trollhack.gui

import dev.luna5ama.trollhack.gui.rgui.MouseState
import dev.luna5ama.trollhack.gui.rgui.WindowComponent
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet

interface IGuiScreen {
    val isVisible: Boolean

    val mouseState: MouseState
    var lastClicked: WindowComponent?
    val hovered: WindowComponent?
    val windows: ObjectLinkedOpenHashSet<WindowComponent>
    val mousePos: Vec2f

    fun closeWindow(window: WindowComponent) {
        if (windows.remove(window)) {
            window.onClosed()
        }
        if (lastClicked === window) lastClicked = null
    }

    fun displayWindow(window: WindowComponent) {
        if (windows.addAndMoveToLast(window)) {
            window.onDisplayed()
        }
    }
}