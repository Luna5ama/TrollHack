package dev.luna5ama.trollhack.util.extension

import it.unimi.dsi.fastutil.longs.Long2LongMap
import it.unimi.dsi.fastutil.longs.Long2LongMaps
import java.util.*

fun <K, V> SortedMap<K, V>.firstKeyOrNull(): K? =
    try {
        firstKey()
    } catch (e: NoSuchElementException) {
        null
    }

fun <K, V> NavigableMap<K, V>.lastValueOrNull(): V? =
    this.lastEntry()?.value

fun <K, V> NavigableMap<K, V>.firstValueOrNull(): V? =
    this.firstEntryOrNull()?.value

fun <K, V> NavigableMap<K, V>.firstEntryOrNull(): MutableMap.MutableEntry<K, V>? =
    firstEntry()

fun <K, V> NavigableMap<K, V>.lastEntryOrNull(): MutableMap.MutableEntry<K, V>? =
    lastEntry()

fun <K, V> MutableMap<K, V>.synchronized(): MutableMap<K, V> =
    Collections.synchronizedMap(this)

fun <K, V> SortedMap<K, V>.synchronized(): SortedMap<K, V> =
    Collections.synchronizedSortedMap(this)

fun <K, V> NavigableMap<K, V>.synchronized(): NavigableMap<K, V> =
    Collections.synchronizedNavigableMap(this)

fun Long2LongMap.synchronized(): Long2LongMap =
    Long2LongMaps.synchronize(this)