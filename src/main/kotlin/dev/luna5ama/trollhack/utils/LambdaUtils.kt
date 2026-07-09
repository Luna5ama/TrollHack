package dev.luna5ama.trollhack.utils

import dev.luna5ama.trollhack.config.settings.AbstractSetting
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun interface Consumer<P> : (P) -> Unit

fun interface Combiner<P> : (P, P) -> P

fun interface Predicate<out P> : (@UnsafeVariance P) -> Boolean

fun interface BiPredicate<P1, P2> : (P1, P2) -> Boolean

fun interface Runnable : () -> Unit

fun <T> refl(): (T) -> T = { it }

fun <T> reflBi(): Combiner<T> = Combiner { _, value -> value }

fun <T> always(): Predicate<T> = Predicate { true }

fun <T1, T2> alwaysBi(): BiPredicate<T1, T2> = BiPredicate { _, _ -> true }

fun <T> never(): Predicate<T> = Predicate { false }

fun <T1, T2> neverBi(): BiPredicate<T1, T2> = BiPredicate { _, _ -> false }

val BOOLEAN_SUPPLIER_FALSE = { false }

fun <T : Any> AbstractSetting<T, *>.notAtValue(value: T): Predicate<Nothing> {
    return Predicate { _ ->
        this.value != value
    }
}


fun <T : Any> AbstractSetting<T, *>.atValue(value: T): Predicate<Nothing> {
    return Predicate { _: T ->
        this.value == value
    }
}

fun <T : Any> AbstractSetting<T, *>.atValue(value1: T, value2: T): Predicate<T> {
    return Predicate { _: T ->
        this.value == value1 || this.value == value2
    }
}

fun AbstractSetting<Boolean, *>.atTrue(): Predicate<Nothing> {
    return Predicate {
        this.value
    }
}

fun AbstractSetting<Boolean, *>.atFalse(): Predicate<Nothing> {
    return Predicate {
        !this.value
    }
}


fun <T : Any> (() -> T).atValue(value: T): Predicate<Nothing> {
    return Predicate {
        this.invoke() == value
    }
}

//internal inline fun <reified T : Any> newInstance(): T {
//    return UnsafeUtils.forceCreateInstance(T::class.java)
//}

//internal inline infix fun <reified T : Any, reified U: Any> ((T) -> Boolean).and(crossinline other: (U) -> Boolean): Predicate<Nothing> {
//    return Predicate {
//        this(newInstance<T>()) && other(newInstance<U>())
//    }
//}


@OptIn(ExperimentalContracts::class)
inline fun <T> T.runIf(boolean: Boolean, block: T.() -> T): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return if (boolean) block.invoke(this) else this
}