package dev.luna5ama.trollhack.gui.hudgui.elements.misc

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.TpsCalculator
import dev.fastmc.common.collection.CircularArray
import dev.fastmc.common.collection.CircularArray.Companion.average

internal object TPS : LabelHud(
    name = "TPS",
    category = Category.MISC,
    description = "Server TPS"
) {

    // Buffered TPS readings to add some fluidity to the TPS HUD element
    private val tpsBuffer = CircularArray(120, 20.0f)

    override fun SafeClientEvent.updateText() {
        tpsBuffer.add(TpsCalculator.tickRate)

        displayText.add("%.2f".format(tpsBuffer.average()), GuiSetting.text)
        displayText.add("tps", GuiSetting.primary)
    }

}