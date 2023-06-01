package dev.luna5ama.trollhack.module.modules.client

import dev.luna5ama.trollhack.event.events.ShutdownEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.gui.hudgui.TrollHudGui
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.threads.onMainThreadSafe

internal object HudEditor : Module(
    name = "Hud Editor",
    description = "Edits the Hud",
    category = Category.CLIENT,
    visible = false
) {
    init {
        onEnable {
            onMainThreadSafe {
                if (mc.currentScreen !is TrollHudGui) {
                    ClickGUI.disable()
                    mc.displayGuiScreen(TrollHudGui)
                    TrollHudGui.onDisplayed()
                }
            }
        }

        onDisable {
            onMainThreadSafe {
                if (mc.currentScreen is TrollHudGui) {
                    mc.displayGuiScreen(null)
                }
            }
        }

        listener<ShutdownEvent> {
            disable()
        }
    }
}