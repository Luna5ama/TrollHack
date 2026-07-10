package dev.luna5ama.trollhack.modules.impl.client

import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.impl.world.WorldEvent
import dev.luna5ama.trollhack.gui.NullClickGui
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import org.lwjgl.glfw.GLFW

object ClickGui : Module("Click Gui", category = Category.CLIENT, defaultBind = GLFW.GLFW_KEY_I) {
    val pauseGame by setting("Pause Game", false)
    init {
        handler<WorldEvent.Load> {
            disable()
        }

        onEnabled {
            HudEditor.disable()
            NullClickGui.open()
        }

        onDisabled {
//            MinecraftClient.getInstance().setScreenAndRender(null)
//            Coroutine.launch {
//                ConfigManager.save()
//            }
        }
    }
}
