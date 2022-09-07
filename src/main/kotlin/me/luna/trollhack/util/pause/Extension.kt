package me.luna.trollhack.util.pause

import me.luna.trollhack.module.AbstractModule
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <R> IPause.withPause(module: AbstractModule, crossinline block: () -> R): R? {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return if (requestPause(module)) block.invoke()
    else null
}

inline fun <R> PriorityTimeoutPause.withPause(module: AbstractModule, timeout: Int, crossinline block: () -> R): R? =
    withPause(module, timeout.toLong(), block)

@OptIn(ExperimentalContracts::class)
inline fun <R> PriorityTimeoutPause.withPause(module: AbstractModule, timeout: Long, crossinline block: () -> R): R? {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return if (requestPause(module, timeout)) block.invoke()
    else null
}