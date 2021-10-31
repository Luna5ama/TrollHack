package cum.xiaro.trollhack.setting.settings.impl.number

import com.google.gson.JsonElement
import cum.xiaro.trollhack.util.asDoubleOrNull
import kotlin.reflect.KProperty

class DoubleSetting(
    name: CharSequence,
    value: Double,
    range: ClosedFloatingPointRange<Double>,
    step: Double,
    visibility: ((() -> Boolean))? = null,
    consumer: (prev: Double, input: Double) -> Double = { _, input -> input },
    description: CharSequence = "",
    fineStep: Double = step
) : NumberSetting<Double>(name, value, range, step, visibility, consumer, description, fineStep) {

    init {
        consumers.add(0) { _, it ->
            it.coerceIn(range)
        }
    }

    override fun read(jsonElement: JsonElement) {
        jsonElement.asDoubleOrNull?.let { value = it }
    }

    override fun setValue(valueIn: Double) {
        value = valueIn
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Double {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) {
        this.value = value
    }
}