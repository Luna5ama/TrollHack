package cum.xiaro.trollhack.setting.settings.impl.primitive

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import cum.xiaro.trollhack.setting.settings.MutableNonPrimitive
import cum.xiaro.trollhack.setting.settings.MutableSetting
import cum.xiaro.trollhack.util.asStringOrNull

sealed class AbstractCharSequenceSetting<T : CharSequence>(
    name: CharSequence,
    value: T,
    visibility: ((() -> Boolean))? = null,
    consumer: (prev: T, input: T) -> T = { _, input -> input },
    description: CharSequence = ""
) : MutableSetting<T>(name, value, visibility, consumer, description), MutableNonPrimitive<T> {
    val stringValue
        get() = value.toString()

    override fun write() = JsonPrimitive(value.toString())

    override fun read(jsonElement: JsonElement) {
        jsonElement.asStringOrNull?.let { setValue(it) }
    }
}

class CharSequenceSetting(
    name: CharSequence,
    value: CharSequence,
    visibility: ((() -> Boolean))? = null,
    consumer: (prev: CharSequence, input: CharSequence) -> CharSequence = { _, input -> input },
    description: CharSequence = ""
) : AbstractCharSequenceSetting<CharSequence>(name, value, visibility, consumer, description) {
    override fun setValue(valueIn: String) {
        value = valueIn
    }
}

class StringSetting(
    name: CharSequence,
    value: String,
    visibility: ((() -> Boolean))? = null,
    consumer: (prev: String, input: String) -> String = { _, input -> input },
    description: CharSequence = ""
) : AbstractCharSequenceSetting<String>(name, value, visibility, consumer, description) {
    override fun setValue(valueIn: String) {
        value = valueIn
    }
}