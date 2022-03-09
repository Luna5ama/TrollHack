package me.luna.trollhack.gui.hudgui.elements.misc

import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.gui.hudgui.LabelHud
import me.luna.trollhack.util.InfoCalculator

internal object Ping : LabelHud(
    name = "Ping",
    category = Category.MISC,
    description = "Delay between client and server"
) {

    override fun SafeClientEvent.updateText() {
        displayText.add(InfoCalculator.ping().toString(), primaryColor)
        displayText.add("ms", secondaryColor)
    }

}