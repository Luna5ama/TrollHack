package dev.luna5ama.trollhack.config.settings

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.luna5ama.trollhack.utils.BiPredicate
import dev.luna5ama.trollhack.utils.Combiner
import dev.luna5ama.trollhack.utils.Predicate
import dev.luna5ama.trollhack.i18n.I18N

class DoubleSetting(
    translateKey: String, i18N: I18N,
    defaultValue: Double, range: ClosedRange<Double>, step: Double = 0.1,
    description: String = "",
    visibility: Predicate<Double>,
    onModified: MutableList<BiPredicate<Double, Double>> = mutableListOf(),
    transformer: Combiner<Double>,
    defaultName: String = translateKey
) : AbstractSteppingRangedSetting<Double, DoubleSetting>(
    translateKey, i18N,
    defaultValue, range, step,
    description, visibility,
    onModified, transformer,
    defaultName
) {
    override fun writeJson(json: JsonObject) {
        json.addProperty(nameAsString, value)
    }

    override fun readJson(json: JsonElement) {
        value = json.asDouble
    }
}