package cum.xiaro.trollhack.module.modules.client

import cum.xiaro.trollhack.event.events.ShutdownEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.gui.hudgui.TrollHudGui
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module

internal object HudEditor : Module(
    name = "HudEditor",
    description = "Edits the Hud",
    category = Category.CLIENT,
    visible = false
) {
    init {
        onEnable {
            if (mc.currentScreen !is TrollHudGui) {
                ClickGUI.disable()
                mc.displayGuiScreen(TrollHudGui)
                TrollHudGui.onDisplayed()
            }
        }

        onDisable {
            if (mc.currentScreen is TrollHudGui) {
                mc.displayGuiScreen(null)
            }
        }

        listener<ShutdownEvent> {
            disable()
        }
    }
}
