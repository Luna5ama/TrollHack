package cum.xiaro.trollhack.gui.hudgui.elements.client

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.gui.hudgui.LabelHud
import cum.xiaro.trollhack.module.modules.movement.AutoWalk
import cum.xiaro.trollhack.process.PauseProcess
import cum.xiaro.trollhack.util.BaritoneUtils

internal object BaritoneProcess : LabelHud(
    name = "BaritoneProcess",
    category = Category.CLIENT,
    description = "Shows what Baritone is doing"
) {

    override fun SafeClientEvent.updateText() {
        val process = BaritoneUtils.primary?.pathingControlManager?.mostRecentInControl()?.orElse(null) ?: return

        when {
            process == PauseProcess -> {
                displayText.addLine(process.displayName0())
            }
            AutoWalk.baritoneWalk -> {
                displayText.addLine("AutoWalk (${AutoWalk.direction.displayName})")
            }
            else -> {
                displayText.addLine("Process: ${process.displayName()}")
            }
        }
    }

}