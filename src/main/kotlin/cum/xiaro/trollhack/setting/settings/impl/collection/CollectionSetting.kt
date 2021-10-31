package cum.xiaro.trollhack.setting.settings.impl.collection

import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import cum.xiaro.trollhack.setting.settings.ImmutableSetting
import cum.xiaro.trollhack.util.asJsonArrayOrNull
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class CollectionSetting<E : Any, T : MutableCollection<E>>(
    name: CharSequence,
    override val value: T,
    visibility: ((() -> Boolean))? = null,
    description: CharSequence = "",
) : ImmutableSetting<T>(name, value, visibility, { _, input -> input }, description), MutableCollection<E> by value {

    override val defaultValue: T = valueClass.newInstance()
    private val lockObject = Any()
    private val type = TypeToken.getArray(value.first().javaClass).type
    val editListeners = ArrayList<(T) -> Unit>()

    init {
        value.toCollection(defaultValue)
    }

    @OptIn(ExperimentalContracts::class)
    inline fun editValue(block: (CollectionSetting<E, T>) -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        block.invoke(this)
        editListeners.forEach { it.invoke(value) }
    }

    override fun resetValue() {
        synchronized(lockObject) {
            editValue {
                value.clear()
                value.addAll(defaultValue)
            }
        }
    }

    override fun write(): JsonElement = gson.toJsonTree(value)

    override fun read(jsonElement: JsonElement) {
        jsonElement.asJsonArrayOrNull?.let {
            val cacheArray = gson.fromJson<Array<E>>(it, type)
            synchronized(lockObject) {
                editValue {
                    value.clear()
                    value.addAll(cacheArray)
                }
            }
        }
    }

    override fun toString() = value.joinToString { it.toString() }

}