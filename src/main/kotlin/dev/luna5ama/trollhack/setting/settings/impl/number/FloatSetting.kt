package dev.luna5ama.trollhack.setting.settings.impl.number

import com.google.gson.JsonElement
import dev.luna5ama.trollhack.util.asFloatOrNull
import kotlin.reflect.KProperty

class FloatSetting(
    name: CharSequence,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    visibility: ((() -> Boolean))? = null,
    consumer: (prev: Float, input: Float) -> Float = { _, input -> input },
    description: CharSequence = "",
    fineStep: Float = step,
    override val isTransient: Boolean = false
) : NumberSetting<Float>(name, value, range, step, visibility, consumer, description, fineStep) {

    init {
        consumers.add(0) { _, it ->
            it.coerceIn(range)
        }
    }

    override fun read(jsonElement: JsonElement) {
        jsonElement.asFloatOrNull?.let { value = it }
    }

    override fun setValue(valueIn: Double) {
        value = valueIn.toFloat()
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Float {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
        this.value = value
    }
}