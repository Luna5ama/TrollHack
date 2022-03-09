package me.luna.trollhack.util.extension

import me.luna.trollhack.util.interfaces.DisplayEnum

fun <E : Enum<E>> E.next(): E = declaringClass.enumConstants.run {
    get((ordinal + 1) % size)
}

fun Enum<*>.readableName() = (this as? DisplayEnum)?.displayName
    ?: name.mapEach('_') { it.lowercase().capitalize() }.joinToString(" ")