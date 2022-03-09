package me.luna.trollhack.gui.hudgui.elements.misc

import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.gui.hudgui.LabelHud
import me.luna.trollhack.util.TpsCalculator
import me.luna.trollhack.util.collections.CircularArray
import me.luna.trollhack.util.collections.CircularArray.Companion.average

internal object TPS : LabelHud(
    name = "TPS",
    category = Category.MISC,
    description = "Server TPS"
) {

    // Buffered TPS readings to add some fluidity to the TPS HUD element
    private val tpsBuffer = CircularArray(120, 20.0f)

    override fun SafeClientEvent.updateText() {
        tpsBuffer.add(TpsCalculator.tickRate)

        displayText.add("%.2f".format(tpsBuffer.average()), primaryColor)
        displayText.add("tps", secondaryColor)
    }

}