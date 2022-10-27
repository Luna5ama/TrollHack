package me.luna.trollhack.module.modules.client

import me.luna.trollhack.event.events.ShutdownEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.gui.hudgui.TrollHudGui
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.threads.onMainThreadSafe

internal object HudEditor : Module(
    name = "HudEditor",
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
