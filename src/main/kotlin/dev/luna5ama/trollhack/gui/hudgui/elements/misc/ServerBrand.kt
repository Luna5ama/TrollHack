package dev.luna5ama.trollhack.gui.hudgui.elements.misc

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.module.modules.client.GuiSetting

internal object ServerBrand : LabelHud(
    name = "Server Brand",
    category = Category.MISC,
    description = "Brand / type of the server"
) {

    override fun SafeClientEvent.updateText() {
        if (mc.isIntegratedServerRunning) {
            displayText.add("Singleplayer: " + mc.player?.serverBrand)
        } else {
            val serverBrand = mc.player?.serverBrand ?: "Unknown Server Type"
            displayText.add(serverBrand, GuiSetting.text)
        }
    }

}