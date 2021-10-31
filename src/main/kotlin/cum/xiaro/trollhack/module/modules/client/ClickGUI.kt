package cum.xiaro.trollhack.module.modules.client

import cum.xiaro.trollhack.event.events.ShutdownEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.gui.clickgui.TrollClickGui
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
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
