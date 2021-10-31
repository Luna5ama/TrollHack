package cum.xiaro.trollhack.setting.settings

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

abstract class AbstractSetting<T : Any> : ISetting<T> {

    val listeners = ArrayList<() -> Unit>()
    val valueListeners = ArrayList<(prev: T, input: T) -> Unit>()

    override val isVisible get() = visibility?.invoke() ?: true

    override val isModified get() = this.value != this.defaultValue

    override fun setValue(valueIn: String) {
        read(parser.parse(valueIn))
    }

    override fun toString() = value.toString()

    override fun equals(other: Any?) = this === other
        || (other is AbstractSetting<*>
        && this.valueClass == other.valueClass
        && this.name == other.name
        && this.value == other.value)

    override fun hashCode() = valueClass.hashCode() * 31 +
        name.hashCode() * 31 +
        value.hashCode()

    protected companion object {
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        val parser = JsonParser()
    }
}