package dev.luna5ama.trollhack.config.settings

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.luna5ama.trollhack.utils.BiPredicate
import dev.luna5ama.trollhack.utils.Combiner
import dev.luna5ama.trollhack.utils.Predicate
import dev.luna5ama.trollhack.i18n.I18N

class FloatSetting(
    translateKey: String, i18N: I18N,
    defaultValue: Float, range: ClosedRange<Float>, step: Float = 0.1f,
    description: String = "",
    visibility: Predicate<Float>,
    onModified: MutableList<BiPredicate<Float, Float>> = mutableListOf(),
    transformer: Combiner<Float>,
    defaultName: String = translateKey
) : AbstractSteppingRangedSetting<Float, FloatSetting>(
    translateKey, i18N,
    defaultValue, range, step,
    description, visibility,
    onModified, transformer,
    defaultName
) {
    override fun readJson(json: JsonElement) {
        value = json.asFloat
    }

    override fun writeJson(json: JsonObject) {
        json.addProperty(nameAsString, value)
    }
}