package cum.xiaro.trollhack.setting.settings

import com.google.gson.JsonElement

/**
 * Basic MutableSetting class
 *
 * @param T Type of this setting
 * @param name Name of this setting
 * @param visibility Called by [isVisible]
 * @param consumer Called on setting [value] to process the value input
 * @param description Description of this setting
 */
open class MutableSetting<T : Any>(
    override val name: CharSequence,
    valueIn: T,
    override val visibility: ((() -> Boolean))?,
    consumer: (prev: T, input: T) -> T,
    override val description: CharSequence
) : AbstractSetting<T>(), IMutableSetting<T> {

    override val defaultValue = valueIn
    override var value = valueIn
        set(value) {
            if (value != field) {
                val prev = field
                var new = value

                for (index in consumers.size - 1 downTo 0) {
                    new = consumers[index](prev, new)
                }
                field = new

                valueListeners.forEach { it(prev, field) }
                listeners.forEach { it() }
            }
        }

    override val valueClass: Class<T> = valueIn.javaClass
    val consumers = arrayListOf(consumer)

    final override fun resetValue() {
        value = defaultValue
    }

    override fun write(): JsonElement = gson.toJsonTree(value)

    override fun read(jsonElement: JsonElement) {
        value = gson.fromJson(jsonElement, valueClass)
    }

}