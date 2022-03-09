package me.luna.trollhack.module.modules.client

import me.luna.trollhack.event.events.ShutdownEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.gui.clickgui.TrollClickGui
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import org.lwjgl.input.Keyboard

internal object ClickGUI : Module(
    name = "ClickGUI",
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
            if (mc.currentScreen !is TrollClickGui) {
                HudEditor.disable()
                mc.displayGuiScreen(TrollClickGui)
                TrollClickGui.onDisplayed()
            }
        }

        onDisable {
            if (mc.currentScreen is TrollClickGui) {
                mc.displayGuiScreen(null)
            }
        }

        bind.value.setBind(Keyboard.KEY_Y)
    }
}
