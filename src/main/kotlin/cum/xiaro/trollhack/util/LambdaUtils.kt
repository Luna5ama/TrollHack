package cum.xiaro.trollhack.util

import cum.xiaro.trollhack.setting.settings.AbstractSetting
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

val BOOLEAN_SUPPLIER_FALSE = { false }

fun <T : Any> AbstractSetting<T>.notAtValue(value: T): () -> Boolean {
    return {
        this.value != value
    }
}


fun <T : Any> AbstractSetting<T>.atValue(value: T): () -> Boolean {
    return {
        this.value == value
    }
}

fun <T : Any> AbstractSetting<T>.atValue(value1: T, value2: T): () -> Boolean {
    return {
        this.value == value1 || this.value == value2
    }
}

fun AbstractSetting<Boolean>.atTrue(): () -> Boolean {
    return {
        this.value
    }
}

fun AbstractSetting<Boolean>.atFalse(): () -> Boolean {
    return {
        !this.value
    }
}

infix fun (() -> Boolean).or(block: (() -> Boolean)): () -> Boolean {
    return {
        this.invoke() || block.invoke()
    }
}

infix fun (() -> Boolean).and(block: (() -> Boolean)): () -> Boolean {
    return {
        this.invoke() && block.invoke()
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T> T.runIf(boolean: Boolean, block: T.() -> T): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return if (boolean) block.invoke(this) else this
}