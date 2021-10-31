package cum.xiaro.trollhack.gui.hudgui.elements.player

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.gui.hudgui.LabelHud
import cum.xiaro.trollhack.manager.managers.TimerManager

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