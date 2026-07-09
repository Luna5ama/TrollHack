package dev.luna5ama.trollhack.modules.impl.client

import dev.luna5ama.trollhack.config.settings.EnumSetting
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.impl.world.WorldEvent
import dev.luna5ama.trollhack.gui.NullClickGui
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import org.lwjgl.glfw.GLFW

object ClickGui : Module("Click Gui", category = Category.CLIENT, defaultBind = GLFW.GLFW_KEY_I) {
    val pauseGame by setting("Pause Game", false)
    private val reloadOnEnabled by setting("Reload", false)
    val outline by setting("Outline", true)
    val bottomAlpha by setting("Bottom Alpha", 255, 30..255)
    val animeHeight by setting("Anime Height", 500f, 0.0f..1000f, 50f)
    val animeXOffset by setting("Anime Offset X", 100f, 0f..500f, 10f)
    val animeType0: EnumSetting<AnimeType> = setting("Anime Type", AnimeType.HITORI_GOTOH)
    val animeType: AnimeType by animeType0
    val colorSync by setting("Color Sync", false)
    val mouseScrollSpeed by setting("Mouse Scroll Speed", 10, 1..20)
    val clearBuffer by setting("Clear Buffer", false)

    init {
        animeType0.register { _, it ->
            sync(it)
            true
        }

        handler<WorldEvent.Load> {
            disable()
        }

        onEnabled {
            if (colorSync) {
                sync(animeType)
            }
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

    fun sync(type: AnimeType) {
        Colors.red = type.color.r
        Colors.green = type.color.g
        Colors.blue = type.color.b
        Colors.rainbow = false
    }
}
