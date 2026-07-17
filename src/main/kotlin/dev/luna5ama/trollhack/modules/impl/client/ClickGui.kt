package dev.luna5ama.trollhack.modules.impl.client

import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.impl.world.WorldEvent
import dev.luna5ama.trollhack.gui.TrollClickGui
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import org.lwjgl.glfw.GLFW

object ClickGui : Module("Click Gui", category = Category.CLIENT, defaultBind = GLFW.GLFW_KEY_I) {
    val scaleSetting = setting(
        "Scale",
        1.00f,
        0.50f..4.00f,
        0.05f,
        transformer = { _, value ->
            (if (value > 4.00f) value / 100.00f else value).coerceIn(0.50f, 4.00f)
        }
    )
    val scale by scaleSetting
    val pauseGame by setting("Pause Game", false)
    init {
        handler<WorldEvent.Load> {
            disable()
        }

        onEnabled {
            HudEditor.disable()
            TrollClickGui.open()
        }

        onDisabled {
            if (mc.screen === TrollClickGui) mc.setScreen(null)
        }
    }
}
