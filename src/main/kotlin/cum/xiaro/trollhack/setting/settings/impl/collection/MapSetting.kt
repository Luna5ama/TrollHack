package cum.xiaro.trollhack.setting.settings.impl.collection

import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import cum.xiaro.trollhack.setting.settings.ImmutableSetting

class MapSetting<K : Any, V : Any, T : MutableMap<K, V>>(
    name: CharSequence,
    override val value: T,
    visibility: ((() -> Boolean))? = null,
    description: CharSequence = ""
) : ImmutableSetting<T>(name, value, visibility, { _, input -> input }, description) {
    override val defaultValue: T = valueClass.newInstance()
    private val type = object : TypeToken<Map<K, V>>() {}.type

    init {
        value.toMap(defaultValue)
    }

    override fun resetValue() {
        value.clear()
        value.putAll(defaultValue)
    }

    override fun write(): JsonElement = gson.toJsonTree(value)

    override fun read(jsonElement: JsonElement) {
        val cacheMap = gson.fromJson<Map<K, V>>(jsonElement, type)
        value.clear()
        value.putAll(cacheMap)
    }

    override fun toString() = value.entries.joinToString { "${it.key} to ${it.value}" }
}