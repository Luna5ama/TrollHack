package me.luna.trollhack.gui.hudgui.elements.player

import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.gui.hudgui.LabelHud
import me.luna.trollhack.manager.managers.TimerManager

internal object TimerSpeed : LabelHud(
    name = "TimerSpeed",
    category = Category.PLAYER,
    description = "Client side timer speed"
) {
    override fun SafeClientEvent.updateText() {
        displayText.add("%.2f".format(50.0f / TimerManager.tickLength), primaryColor)
        displayText.add("x", secondaryColor)
    }
}