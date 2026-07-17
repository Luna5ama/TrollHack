package dev.luna5ama.trollhack.config.settings

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.luna5ama.trollhack.config.normalizeSettingName
import dev.luna5ama.trollhack.i18n.I18N
import dev.luna5ama.trollhack.utils.*
import dev.luna5ama.trollhack.i18n.LocalizedNameable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class AbstractSetting<V, S : AbstractSetting<V, S>>(
    translateKey: String, i18N: I18N,
    val defaultValue: V,
    override val description: String,
    val visibility: Predicate<V>,
    protected val onModified: MutableList<BiPredicate<V, V>>,
    protected val transformer: Combiner<V>,
    defaultName: String = translateKey
) : LocalizedNameable(translateKey, i18N, defaultName), Describable, ReadWriteProperty<Any?, V> {
    open var value: V = defaultValue
        set(value) {
            val prev = field
            if (onModified.all {
                it(prev, value)
            }) field = transformer(prev, value)
        }

    val isVisible get() = visibility(value)

    fun clearConsumers() = onModified.clear()

    fun register(consumer: BiPredicate<V, V>): S {
        onModified.add(consumer)
        return this as S
    }

    fun setToDefault() {
        this.value = defaultValue
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): V = value
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        this.value = value
    }

    open fun chooseJsonElement(json: JsonObject): JsonElement? {
        return json[defaultName] ?: json[nameAsString] ?: json[translateKey]
        ?: json.entrySet().firstOrNull { (key, _) ->
            normalizeSettingName(key).replace(" ", "").equals(
                normalizeSettingName(defaultName).replace(" ", ""),
                ignoreCase = true
            )
        }?.value
    }

    abstract fun writeJson(json: JsonObject)
    abstract fun readJson(json: JsonElement)
}
