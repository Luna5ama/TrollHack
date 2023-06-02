package dev.luna5ama.trollhack.setting.settings.impl.number

import com.google.gson.JsonPrimitive
import dev.luna5ama.trollhack.setting.settings.MutableSetting

abstract class NumberSetting<T>(
    name: CharSequence,
    value: T,
    val range: ClosedRange<T>,
    val step: T,
    visibility: ((() -> Boolean))?,
    consumer: (prev: T, input: T) -> T,
    description: CharSequence = "",
    val fineStep: T,
    override val isTransient: Boolean = false
) : MutableSetting<T>(name, value, visibility, consumer, description)
    where T : Number, T : Comparable<T> {

    override fun write() = JsonPrimitive(value)

    final override fun setValue(valueIn: String) {
        valueIn.toDoubleOrNull()?.let {
            setValue(it)
        }
    }

    abstract fun setValue(valueIn: Double)

}
