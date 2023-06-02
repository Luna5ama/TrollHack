package dev.luna5ama.trollhack.setting.settings.impl.primitive

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import dev.luna5ama.trollhack.setting.settings.MutableSetting
import dev.luna5ama.trollhack.util.asBooleanOrNull
import kotlin.reflect.KProperty

open class BooleanSetting(
    name: CharSequence,
    value: Boolean,
    visibility: ((() -> Boolean))? = null,
    consumer: (prev: Boolean, input: Boolean) -> Boolean = { _, input -> input },
    description: CharSequence = "",
    override val isTransient: Boolean = false
) : MutableSetting<Boolean>(name, value, visibility, consumer, description) {

    override fun write(): JsonElement = JsonPrimitive(value)

    override fun read(jsonElement: JsonElement) {
        jsonElement.asBooleanOrNull?.let { value = it }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        this.value = value
    }
}