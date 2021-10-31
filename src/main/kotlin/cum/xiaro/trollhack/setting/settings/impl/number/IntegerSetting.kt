package cum.xiaro.trollhack.setting.settings.impl.number

import com.google.gson.JsonElement
import cum.xiaro.trollhack.util.asIntOrNull
import kotlin.reflect.KProperty

class IntegerSetting(
    name: CharSequence,
    value: Int,
    range: IntRange,
    step: Int,
    visibility: ((() -> Boolean))? = null,
    consumer: (prev: Int, input: Int) -> Int = { _, input -> input },
    description: CharSequence = "",
    fineStep: Int = step
) : NumberSetting<Int>(name, value, range, step, visibility, consumer, description, fineStep) {

    init {
        consumers.add(0) { _, it ->
            it.coerceIn(range)
        }
    }

    override fun read(jsonElement: JsonElement) {
        jsonElement.asIntOrNull?.let { value = it }
    }

    override fun setValue(valueIn: Double) {
        value = valueIn.toInt()
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        this.value = value
    }
}