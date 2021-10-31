package cum.xiaro.trollhack.setting.settings

import cum.xiaro.trollhack.util.interfaces.Nameable
import com.google.gson.JsonElement

interface ISetting<T : Any> : Nameable {
    val value: T
    val defaultValue: T
    val valueClass: Class<T>
    val visibility: (() -> Boolean)?
    val description: CharSequence
    val isVisible: Boolean
    val isModified: Boolean

    fun setValue(valueIn: String)
    fun resetValue()
    fun write(): JsonElement
    fun read(jsonElement: JsonElement)
}

interface IMutableSetting<T : Any> : ISetting<T> {
    override var value: T
}