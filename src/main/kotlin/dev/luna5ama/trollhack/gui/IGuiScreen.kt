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

    fun closeWindow(window: WindowComponent): Boolean {
        var closed = false
        if (windows.remove(window)) {
            window.onClosed()
            closed = true
        }
        if (lastClicked === window) lastClicked = null

        return closed
    }

    fun displayWindow(window: WindowComponent): Boolean {
        return if (windows.addAndMoveToLast(window)) {
            window.onDisplayed()
            true
        } else {
            false
        }
    }
}