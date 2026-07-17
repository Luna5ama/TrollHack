package dev.luna5ama.trollhack.config.settings

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.luna5ama.trollhack.utils.BiPredicate
import dev.luna5ama.trollhack.utils.Combiner
import dev.luna5ama.trollhack.utils.Predicate
import dev.luna5ama.trollhack.i18n.I18N

class BooleanSetting(
    translateKey: String, i18N: I18N,
    defaultValue: Boolean, description: String = "",
    visibility: Predicate<Boolean>,
    onModified: MutableList<BiPredicate<Boolean, Boolean>>,
    transformer: Combiner<Boolean>,
    defaultName: String = translateKey
) : AbstractSetting<Boolean, BooleanSetting>(
    translateKey, i18N,
    defaultValue, description,
    visibility, onModified, transformer,
    defaultName
) {
    override fun readJson(json: JsonElement) {
        value = json.asBoolean
    }

    override fun writeJson(json: JsonObject) {
        json.addProperty(defaultName, value)
    }
}
