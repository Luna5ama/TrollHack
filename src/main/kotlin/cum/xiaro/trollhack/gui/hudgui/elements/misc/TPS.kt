package cum.xiaro.trollhack.gui.hudgui.elements.misc

import cum.xiaro.trollhack.util.collections.CircularArray
import cum.xiaro.trollhack.util.collections.CircularArray.Companion.average
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.gui.hudgui.LabelHud
import cum.xiaro.trollhack.util.TpsCalculator

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