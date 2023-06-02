package dev.luna5ama.trollhack.setting.settings

import com.google.gson.JsonElement
import dev.luna5ama.trollhack.util.interfaces.Nameable

interface ISetting<T : Any> : Nameable {
    val value: T
    val defaultValue: T
    val valueClass: Class<T>
    val visibility: (() -> Boolean)?
    val description: CharSequence
    val isVisible: Boolean
    val isModified: Boolean
    val isTransient: Boolean

    fun setValue(valueIn: String)
    fun resetValue()
    fun write(): JsonElement
    fun read(jsonElement: JsonElement)
}

interface IMutableSetting<T : Any> : ISetting<T> {
    override var value: T
}