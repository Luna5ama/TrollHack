package cum.xiaro.trollhack.gui.hudgui.elements.misc

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.gui.hudgui.LabelHud
import cum.xiaro.trollhack.util.InfoCalculator

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