package dev.luna5ama.trollhack.config.settings

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.luna5ama.trollhack.utils.BiPredicate
import dev.luna5ama.trollhack.utils.Combiner
import dev.luna5ama.trollhack.utils.Predicate
import dev.luna5ama.trollhack.i18n.I18N

class StringSetting(
    translateKey: String, i18N: I18N,
    defaultValue: String, description: String = "",
    visibility: Predicate<String>,
    onModified: MutableList<BiPredicate<String, String>> = mutableListOf(),
    transformer: Combiner<String>,
    defaultName: String = translateKey
) : AbstractSetting<String, StringSetting>(
    translateKey, i18N,
    defaultValue, description,
    visibility, onModified, transformer,
    defaultName
) {
    override fun readJson(json: JsonElement) {
        value = json.asString
    }

    override fun writeJson(json: JsonObject) {
        json.addProperty(defaultName, value)
    }
}
