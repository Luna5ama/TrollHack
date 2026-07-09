package dev.luna5ama.trollhack.utils

import kotlin.contracts.contract

sealed class Option<out T : Any>(protected var value: @UnsafeVariance T?) : Iterable<T> {
    class Some<T : Any>(value: T) : Option<T>(value) {
        override fun get(): T {
            return value!!
        }

        override fun toString(): String {
            return "Some($value)"
        }
    }
    data object None : Option<Nothing>(null) {
        override fun get(): Nothing {
            throw NoSuchElementException("Option is empty")
        }

        override fun toString(): String {
            return "None"
        }
    }

    abstract fun get(): T

    val get get() = get()

    fun isDefined(): Boolean {
        contract {
            returns(true) implies (this@Option is Some<T>)
            returns(false) implies (this@Option is None)
        }
        return !isEmpty()
    }

    fun isEmpty(): Boolean {
        contract {
            returns(false) implies (this@Option is Some<T>)
            returns(true) implies (this@Option is None)
        }
        return this == None
    }

    fun isNotEmpty(): Boolean {
        contract {
            returns(true) implies (this@Option is Some<T>)
            returns(false) implies (this@Option is None)
        }
        return !isEmpty()
    }

    fun getOrElse(t: @UnsafeVariance T) : T {
        return if (isNotEmpty()) value!! else t
    }

    fun getOrElse(t: () -> @UnsafeVariance T) : T {
        return if (isNotEmpty()) value!! else t()
    }

    fun getOrNull() = value

//    fun set(v: @UnsafeVariance T?) {
//        if (v != null) value = v
//    }

    fun orElse(t: Option<@UnsafeVariance T>) : Option<T> {
        return if (isNotEmpty()) this else t
    }

    fun orElse(t: () -> Option<@UnsafeVariance T>): Option<T> {
        return if (isNotEmpty()) this else t()
    }

    override fun iterator(): Iterator<T> {
        return object : Iterator<T> {
            var got = false

            override fun hasNext(): Boolean {
                return value != null && !got
            }

            override fun next(): T {
                return if (got) throw NoSuchElementException("IteratorOnce is used twice") else {
                    got = true
                    value!!
                }
            }
        }
    }

    companion object {
        fun <T> of(t: T): Option<T & Any> = if (t != null) Some(t) else None

        fun <T> empty(): Option<T & Any> = None

        operator fun <T> invoke(t: T): Option<T & Any> = of(t)
    }
}

fun <T> T.toOption() = this?.let { Option(it) } ?: Option.empty()
fun <T : Any> Option<Option<T>>.flattenOption() = getOrNull() ?: Option.empty<T>()
fun <T : Any> Collection<Option<T>>.firstDefined() = firstOrNull { it.isDefined() } ?: Option.empty()
fun <T : Any> Collection<Option<T>>.filterDefined(): List<Option.Some<T>> = filter { it.isNotEmpty() } as List<Option.Some<T>>