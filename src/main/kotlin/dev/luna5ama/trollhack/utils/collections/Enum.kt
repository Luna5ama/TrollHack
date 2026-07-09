package dev.luna5ama.trollhack.utils.collections

import java.util.*

inline fun <reified K : Enum<K>, V> EnumMap(): EnumMap<K, V> {
    return EnumMap<K, V>(K::class.java)
}

inline fun <reified E : Enum<E>> EnumSet(): EnumSet<E> {
    return EnumSet.noneOf(E::class.java)
}