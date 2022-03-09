package me.luna.trollhack.module.modules.client

import me.luna.trollhack.module.AbstractModule
import me.luna.trollhack.module.Category
import me.luna.trollhack.setting.GenericConfig
import me.luna.trollhack.translation.TranslationManager

internal object Language : AbstractModule(
    name = "Language",
    description = "Change language",
    category = Category.CLIENT,
    alwaysEnabled = true,
    visible = false,
    config = GenericConfig
) {
    private val overrideLanguage0 = setting("Override Language", false)
    private val language0 = setting("Language", "en_us", { overrideLanguage })

    val overrideLanguage by overrideLanguage0
    val language by language0

    init {
        overrideLanguage0.listeners.add {
            TranslationManager.reload()
        }

        language0.listeners.add {
            TranslationManager.reload()
        }
    }
}