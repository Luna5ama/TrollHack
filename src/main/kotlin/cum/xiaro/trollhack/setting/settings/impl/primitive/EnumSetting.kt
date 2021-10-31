package cum.xiaro.trollhack.setting.settings.impl.primitive

import cum.xiaro.trollhack.util.extension.next
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import cum.xiaro.trollhack.setting.settings.MutableNonPrimitive
import cum.xiaro.trollhack.setting.settings.MutableSetting
import cum.xiaro.trollhack.util.asStringOrNull
import java.util.*

class EnumSetting<T : Enum<T>>(
    name: CharSequence,
    value: T,
    visibility: ((() -> Boolean))? = null,
    consumer: (prev: T, input: T) -> T = { _, input -> input },
    description: CharSequence = ""
) : MutableSetting<T>(name, value, visibility, consumer, description), MutableNonPrimitive<T> {

    val enumClass: Class<T> = value.declaringClass
    val enumValues: Array<out T> = enumClass.enumConstants

    fun nextValue() {
        value = value.next()
    }

    override fun setValue(valueIn: String) {
        super<MutableSetting>.setValue(valueIn.uppercase(Locale.ROOT).replace(' ', '_'))
    }

    override fun write(): JsonElement = JsonPrimitive(value.name)

    override fun read(jsonElement: JsonElement) {
        jsonElement.asStringOrNull?.let { element ->
            enumValues.firstOrNull { it.name.equals(element, true) }?.let {
                value = it
            }
        }
    }

}