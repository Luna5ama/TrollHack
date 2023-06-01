package dev.luna5ama.trollhack.module.modules.client

import dev.luna5ama.trollhack.event.events.ShutdownEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.gui.clickgui.TrollClickGui
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.threads.onMainThreadSafe
import org.lwjgl.input.Keyboard

internal object ClickGUI : Module(
    name = "Click GUI",
    description = "Opens the Click GUI",
    category = Category.CLIENT,
    visible = false,
    alwaysListening = true
) {
    init {
        listener<ShutdownEvent> {
            disable()
        }

        onEnable {
            onMainThreadSafe {
                if (mc.currentScreen !is TrollClickGui) {
                    HudEditor.disable()
                    mc.displayGuiScreen(TrollClickGui)
                    TrollClickGui.onDisplayed()
                }
            }
        }

        onDisable {
            onMainThreadSafe {
                if (mc.currentScreen is TrollClickGui) {
                    mc.displayGuiScreen(null)
                }
            }
        }

        bind.value.setBind(Keyboard.KEY_Y)
    }
}