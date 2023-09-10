package dev.luna5ama.trollhack.gui

import dev.fastmc.common.collection.FastObjectArrayList
import dev.luna5ama.trollhack.gui.rgui.MouseState
import dev.luna5ama.trollhack.gui.rgui.WindowComponent
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import dev.luna5ama.trollhack.util.threads.runSynchronized
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
        if (windows.runSynchronized { remove(window) }) {
            window.onClosed()
            closed = true
        }
        if (lastClicked === window) lastClicked = null

        return closed
    }

    fun displayWindow(window: WindowComponent): Boolean {
        return if (windows.runSynchronized { addAndMoveToLast(window) }) {
            window.onDisplayed()
            true
        } else {
            false
        }
    }

    val windowsCachedList: FastObjectArrayList<WindowComponent>

    companion object {
        inline fun IGuiScreen.forEachWindow(crossinline block: (WindowComponent) -> Unit) {
            windows.runSynchronized { windowsCachedList.addAll(this) }
            for (i in windowsCachedList.indices) {
                block(windowsCachedList[i])
            }
            windowsCachedList.clear()
        }
    }
}