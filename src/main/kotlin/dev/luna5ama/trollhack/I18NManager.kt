package dev.luna5ama.trollhack

import dev.luna5ama.trollhack.i18n.I18N
import dev.luna5ama.trollhack.i18n.Lang
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.ResourceHelper
import java.io.File

object I18NManager {
    val currentLanguage: Lang get() = ClientSettings.modLanguage
    val i18N = I18N(mapOf()) { currentLanguage }

    fun read(name: String): I18N {
        val languageDir = File(".").resolve(name).resolve("i18n")
        if (!languageDir.exists()) languageDir.mkdirs()

        fun extractLangFile(lang: Lang) {
            val langFile = languageDir.resolve(lang.key + ".lang")
            if (langFile.exists()) langFile.delete()
            ResourceHelper.extractFromResourcePath("/assets/trollhack/lang/${lang.key}.lang")?.copyTo(langFile)
        }

        if (languageDir.listFiles()?.isEmpty() == true || TrollHackMod.USE_DEFAULT_LANG) {
            Lang.entries.forEach(::extractLangFile)
        }

        val languages = buildMap {
            Lang.entries.forEach { lang ->
                val languageFile = languageDir.resolve(lang.key + ".lang")
                if (languageFile.exists()) put(lang, languageFile.readText())
            }
        }

        i18N.update(languages)
        return i18N
    }
}