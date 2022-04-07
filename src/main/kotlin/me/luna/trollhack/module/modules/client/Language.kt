package me.luna.trollhack.module.modules.client

import me.luna.trollhack.event.events.GuiEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.module.AbstractModule
import me.luna.trollhack.module.Category
import me.luna.trollhack.setting.GenericConfig
import me.luna.trollhack.translation.TranslationManager
import me.luna.trollhack.util.Wrapper
import net.minecraft.client.gui.GuiMainMenu

internal object Language : AbstractModule(
    name = "Language",
    description = "Change language",
    category = Category.CLIENT,
    alwaysEnabled = true,
    visible = false,
    config = GenericConfig
) {
    private val overrideLanguage = setting("Override Language", false)
    private val language = setting("Language", "en_us", { overrideLanguage.value })

    init {
        listener<GuiEvent.Displayed>(114514) {
            if (it.screen is GuiMainMenu || it.screen is MainMenu.TrollGuiMainMenu) {
                TranslationManager.reload()
            }
        }
    }

    val settingLanguage: String
        get() = if (overrideLanguage.value) {
            language.value
        } else {
            Wrapper.minecraft.gameSettings.language
        }

    init {
        overrideLanguage.listeners.add {
            TranslationManager.reload()
        }

        language.listeners.add {
            TranslationManager.reload()
        }
    }
}