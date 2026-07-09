package dev.luna5ama.trollhack.modules.impl.client

import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.impl.world.WorldEvent
import dev.luna5ama.trollhack.gui.NullHudEditor
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import org.lwjgl.glfw.GLFW

object HudEditor : Module("Hud Editor", category = Category.CLIENT, defaultBind = GLFW.GLFW_KEY_UNKNOWN) {
    val pauseGame by setting("Pause Game", false)
    private val reloadOnEnabled by setting("Reload", false)
    val outline by setting("Outline", true)
    val mouseScrollSpeed by setting("Mouse Scroll Speed", 10, 1..20)
    val fix by setting("Fix Gui", false)

    init {
        handler<WorldEvent.Load> {
            disable()
        }

        onEnabled {
            NullHudEditor.open()
        }

        onDisabled {
//            MinecraftClient.getInstance().setScreenAndRender(null)
//            Coroutine.launch {
//                ConfigManager.save()
//            }
        }
    }
}
