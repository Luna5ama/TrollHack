package dev.luna5ama.trollhack

import dev.luna5ama.trollhack.i18n.I18N
import dev.luna5ama.trollhack.i18n.Lang
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.ResourceHelper
import java.io.File

object I18NManager {
    private const val LEGACY_NAMESPACE = "nullhack-nextgen"
    val currentLanguage: Lang get() = ClientSettings.modLanguage
    val i18N = I18N(mapOf()) { currentLanguage }

    fun read(name: String): I18N {
        val languageDir = File(".").resolve(name).resolve("i18n")
        if (!languageDir.exists()) languageDir.mkdirs()

        fun jsonFile(lang: Lang) = languageDir.resolve("${lang.key}.json")
        fun migrateLegacyFile(lang: Lang) {
            val legacyFile = languageDir.resolve("${lang.key}.lang")
            val jsonFile = jsonFile(lang)
            if (!jsonFile.exists() && legacyFile.exists()) {
                jsonFile.writeText(I18N.migrateLegacyFile(legacyFile.readText()) { key ->
                    when {
                        key == LEGACY_NAMESPACE -> name
                        key.startsWith("$LEGACY_NAMESPACE.") -> "$name${key.removePrefix(LEGACY_NAMESPACE)}"
                        else -> key
                    }
                })
            }
        }

        fun extractLanguageFile(lang: Lang) {
            ResourceHelper.extractFromResourcePath("/assets/trollhack/i18n/${lang.key}.json")
                ?.copyTo(jsonFile(lang), overwrite = true)
        }

        if (languageDir.listFiles()?.isEmpty() == true || TrollHackMod.USE_DEFAULT_LANG) {
            Lang.entries.forEach(::extractLanguageFile)
        }

        val languages = buildMap {
            Lang.entries.forEach { lang ->
                migrateLegacyFile(lang)
                val languageFile = jsonFile(lang)
                if (languageFile.exists()) put(lang, languageFile.readText())
            }
        }

        i18N.update(languages)
        return i18N
    }
}
