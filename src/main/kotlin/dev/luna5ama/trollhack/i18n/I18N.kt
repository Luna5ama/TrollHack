package dev.luna5ama.trollhack.i18n

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

class I18N(languageFiles: Map<Lang, String>, private val lang: () -> Lang) {
    private val texts = Int2ObjectOpenHashMap<I18NText>()
    val currentLang get() = lang()

    init {
        update(languageFiles)
    }

    operator fun get(translateKey: String, defaultText: String = translateKey): I18NText {
        val legacyKey = translateKey.replace(" ", "")
        val fallback = if (legacyKey == translateKey) null else texts[legacyKey.hashCode()]
        return texts.computeIfAbsent(translateKey.hashCode(), Int2ObjectFunction {
            I18NText(translateKey, this, defaultText, fallback)
        }).also { it.defaultText = defaultText }
    }

    fun update(languageFiles: Map<Lang, String>) {
        languageFiles.forEach { (lang, file) ->
            parseJson(file).forEach { (key, value) ->
                this[key][lang] = value
            }
        }
    }

    fun dump(): Map<Lang, String> = buildMap {
        Lang.entries.forEach { lang ->
            put(lang, toJson(texts.values.map { it.translateKey to it[lang] }.toMap()))
        }
    }

    companion object {
        private const val VALUE_KEY = "_value"
        private const val COMMENT_KEY = "_comment"
        private val gson = GsonBuilder().setPrettyPrinting().create()

        fun migrateLegacyFile(file: String, transformKey: (String) -> String = { it }): String = toJson(buildMap {
            parseLegacyFile(file).forEach { (key, value) ->
                val transformedKey = transformKey(key)
                require(put(transformedKey, value) == null) {
                    "Multiple legacy I18N entries map to '$transformedKey'"
                }
            }
        })

        private fun parseJson(file: String): Map<String, String> {
            val root = JsonParser.parseString(file)
            require(root.isJsonObject) { "I18N file root must be a JSON object" }

            return buildMap {
                flatten(root, emptyList(), this)
            }
        }

        private fun flatten(element: JsonElement, path: List<String>, output: MutableMap<String, String>) {
            when {
                element.isJsonObject -> element.asJsonObject.entrySet().forEach { (key, value) ->
                    if (key == COMMENT_KEY) {
                        return@forEach
                    } else if (key == VALUE_KEY) {
                        require(path.isNotEmpty()) { "'$VALUE_KEY' is only valid inside an I18N namespace" }
                        output[path.joinToString(".")] = stringValue(value, path)
                    } else {
                        flatten(value, path + key, output)
                    }
                }

                else -> {
                    require(path.isNotEmpty()) { "I18N file must contain at least one translation key" }
                    output[path.joinToString(".")] = stringValue(element, path)
                }
            }
        }

        private fun stringValue(element: JsonElement, path: List<String>): String {
            require(element is JsonPrimitive && element.isString) {
                "I18N value '${path.joinToString(".")}' must be a string or a JSON object"
            }
            return element.asString
        }

        private fun parseLegacyFile(file: String): Map<String, String> = buildMap {
            file.lineSequence().forEach { line ->
                val entry = line.trim()
                if (entry.isNotEmpty() && !entry.startsWith('#')) {
                    val separatorIndex = entry.indexOf('=')
                    require(separatorIndex >= 0) { "Invalid legacy I18N entry: $entry" }
                    put(entry.substring(0, separatorIndex), entry.substring(separatorIndex + 1))
                }
            }
        }

        private fun toJson(translations: Map<String, String>): String {
            val root = TranslationNode()
            translations.toSortedMap().forEach { (key, value) ->
                val node = key.split('.').fold(root) { current, segment ->
                    require(segment.isNotEmpty()) { "I18N keys cannot contain empty path segments: $key" }
                    current.children.getOrPut(segment, ::TranslationNode)
                }
                node.value = value
            }
            return gson.toJson(root.toJsonObject())
        }

        private class TranslationNode {
            val children = sortedMapOf<String, TranslationNode>()
            var value: String? = null

            fun toJsonElement(): JsonElement = if (children.isEmpty()) {
                JsonPrimitive(requireNotNull(value))
            } else {
                toJsonObject()
            }

            fun toJsonObject(): JsonObject = JsonObject().also { json ->
                value?.let { json.addProperty(VALUE_KEY, it) }
                children.forEach { (key, child) -> json.add(key, child.toJsonElement()) }
            }
        }
    }
}
