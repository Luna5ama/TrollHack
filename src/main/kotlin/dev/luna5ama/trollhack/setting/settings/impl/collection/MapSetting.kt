package dev.luna5ama.trollhack.setting.settings.impl.collection

import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import dev.luna5ama.trollhack.setting.settings.ImmutableSetting

class MapSetting<K, V, T : MutableMap<K, V>>(
    name: CharSequence,
    override val value: T,
    private val typeToken: TypeToken<T>,
    visibility: ((() -> Boolean))? = null,
    description: CharSequence = "",
    override val isTransient: Boolean = false
) : ImmutableSetting<T>(name, value, visibility, { _, input -> input }, description) {
    override val defaultValue: T = valueClass.newInstance()

    init {
        value.toMap(defaultValue)
    }

    override fun resetValue() {
        value.clear()
        value.putAll(defaultValue)
    }

    override fun write(): JsonElement = gson.toJsonTree(value)

    override fun read(jsonElement: JsonElement) {
        val cacheMap = gson.fromJson<Map<K, V>>(jsonElement, typeToken.type)
        value.clear()
        value.putAll(cacheMap)
    }

    override fun toString() = value.entries.joinToString { "${it.key} to ${it.value}" }

    companion object {
        inline operator fun <reified K : Any, reified V : Any, reified T : MutableMap<K, V>> invoke(
            name: CharSequence,
            value: T,
            noinline visibility: ((() -> Boolean))? = null,
            description: CharSequence = ""
        ) = MapSetting(name, value, object : TypeToken<T>() {}, visibility, description)
    }
}