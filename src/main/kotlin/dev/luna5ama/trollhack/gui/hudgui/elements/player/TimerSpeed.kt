package dev.luna5ama.trollhack.gui.hudgui.elements.player

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.manager.managers.TimerManager
import dev.luna5ama.trollhack.module.modules.client.GuiSetting

internal object TimerSpeed : LabelHud(
    name = "Timer Speed",
    category = Category.PLAYER,
    description = "Client side timer speed"
) {
    override fun SafeClientEvent.updateText() {
        displayText.add("%.2f".format(50.0f / TimerManager.tickLength), GuiSetting.text)
        displayText.add("x", GuiSetting.primary)
    }
}