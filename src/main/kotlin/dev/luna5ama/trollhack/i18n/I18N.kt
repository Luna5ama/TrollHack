package dev.luna5ama.trollhack.i18n

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

class I18N(languageFile: Map<Lang, String>, private val lang: () -> Lang) {
    private val texts = Int2ObjectOpenHashMap<I18NText>()
    val currentLang get() = lang()

    init {
        languageFile.forEach { (lang, file) ->
            file.split("\n").forEach {
                if (it.isNotBlank() && !it.startsWith("#")) {
                    val (key, value) = it.trim().split("=") + ""
                    this[key][lang] = value
                }
            }
        }
    }

    operator fun get(translateKey: String, defaultText: String = translateKey): I18NText =
        texts.computeIfAbsent(translateKey.hashCode(), Int2ObjectFunction {
            I18NText(translateKey, this, defaultText)
        }).also { it.defaultText = defaultText }

    fun update(languageFile: Map<Lang, String>) {
        languageFile.forEach { (lang, file) ->
            file.split("\n").forEach {
                if (it.isNotBlank() && !it.startsWith("#")) {
                    val (key, value) = it.trim().split("=") + ""
                    this[key][lang] = value
                }
            }
        }
    }

    fun dump(): Map<Lang, String> = buildMap {
        Lang.entries.forEach { lang ->
            val lines = buildList {
                texts.forEach { (_, i18NText) ->
                    add("${i18NText.translateKey}=${i18NText[lang]}")
                }
            }
            put(lang, lines.sorted().joinToString("\n"))
        }
    }
}