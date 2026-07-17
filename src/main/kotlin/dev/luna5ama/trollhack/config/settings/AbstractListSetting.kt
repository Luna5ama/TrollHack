package dev.luna5ama.trollhack.config.settings

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.luna5ama.trollhack.i18n.I18N
import dev.luna5ama.trollhack.utils.BiPredicate
import dev.luna5ama.trollhack.utils.Combiner
import dev.luna5ama.trollhack.utils.Predicate

abstract class AbstractListSetting<E>(
    translateKey: String, i18N: I18N,
    defaultValue: List<E>, description: String,
    visibility: Predicate<List<E>>,
    onModified: MutableList<BiPredicate<List<E>, List<E>>>,
    transformer: Combiner<List<E>>,
    defaultName: String = translateKey
) : AbstractSetting<List<E>, AbstractListSetting<E>>(
    translateKey, i18N,
    defaultValue, description,
    visibility, onModified, transformer,
    defaultName
) {
    override fun readJson(json: JsonElement) {
        value = buildList {
            json.asJsonArray.forEach {
                add(string2Element(it.asString))
            }
        }
    }

    override fun writeJson(json: JsonObject) {
        val array = JsonArray().apply {
            value.forEach {
                add(element2String(it))
            }
        }
        json.add(defaultName, array)
    }

    abstract fun element2String(element: E): String
    abstract fun string2Element(string: String): E
}
