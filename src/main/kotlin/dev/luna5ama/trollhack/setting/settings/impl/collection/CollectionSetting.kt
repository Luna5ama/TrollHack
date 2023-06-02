package dev.luna5ama.trollhack.setting.settings.impl.collection

import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import dev.luna5ama.trollhack.setting.settings.ImmutableSetting
import dev.luna5ama.trollhack.util.asJsonArrayOrNull
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class CollectionSetting<E, T : MutableCollection<E>>(
    name: CharSequence,
    override val value: T,
    private val typeToken: TypeToken<*>,
    visibility: ((() -> Boolean))? = null,
    description: CharSequence = "",
    override val isTransient: Boolean = false
) : ImmutableSetting<T>(name, value, visibility, { _, input -> input }, description), MutableCollection<E> by value {

    override val defaultValue: T = valueClass.newInstance()
    private val lockObject = Any()

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
            val cacheArray = gson.fromJson<Array<E>>(it, typeToken.type)
            synchronized(lockObject) {
                editValue {
                    value.clear()
                    value.addAll(cacheArray)
                }
            }
        }
    }

    override fun toString() = value.joinToString { it.toString() }


    companion object {
        inline operator fun <reified E : Any, reified T : MutableCollection<E>> invoke(
            name: CharSequence,
            value: T,
            noinline visibility: ((() -> Boolean))? = null,
            description: CharSequence = "",
        ) = CollectionSetting(name, value, TypeToken.getArray(E::class.java), visibility, description)
    }
}