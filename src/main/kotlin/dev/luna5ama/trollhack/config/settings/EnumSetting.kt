package dev.luna5ama.trollhack.config.settings

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.luna5ama.trollhack.i18n.I18N
import dev.luna5ama.trollhack.utils.*

class EnumSetting<E>(
    translateKey: String, i18N: I18N,
    defaultValue: E, description: String,
    visibility: Predicate<E>,
    onModified: MutableList<BiPredicate<E, E>>,
    transformer: Combiner<E>,
    defaultName: String = translateKey
) : AbstractSetting<E, EnumSetting<E>>(
    translateKey, i18N,
    defaultValue, description,
    visibility, onModified, transformer,
    defaultName
) where E : Enum<E>, E : Displayable {
    private val values = runCatching { defaultValue.declaringJavaClass.enumConstants.toList() }.getOrElse {
        ((defaultValue::class.java.companion as? IEnumEntriesProvider<E>)
            ?: defaultValue::class.java.superclass.companion as? IEnumEntriesProvider<E>)!!.entries
    }

    fun next() {
        val index = values.indexOf(value)
        value = values[(index + 1) % values.size]
    }

    fun prev() {
        val index = values.indexOf(value)
        value = values[(values.size + index - 1) % values.size]
    }

    fun setWithName(name: String) {
        value = values.find { it.displayName.toString().lowercase() == name.lowercase() } ?: defaultValue
    }

    override fun readJson(json: JsonElement) {
        setWithName(json.asString)
    }

    override fun writeJson(json: JsonObject) {
        json.addProperty(defaultName, value.displayName.toString())
    }
}
