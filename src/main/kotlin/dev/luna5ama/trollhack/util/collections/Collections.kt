package dev.luna5ama.trollhack.util.collections

import it.unimi.dsi.fastutil.longs.LongCollection


inline fun <T> compareFloatBy(crossinline block: (T) -> Float): Comparator<T> {
    return Comparator { a, b ->
        block(a).compareTo(block(b))
    }
}

inline fun <T> compareFloatByDescending(crossinline block: (T) -> Float): Comparator<T> {
    return Comparator { a, b ->
        block(b).compareTo(block(a))
    }
}

inline fun <T> List<T>.forEachFast(action: (T) -> Unit) {
    for (i in indices) {
        action(get(i))
    }
}

inline fun <T> List<T>.none(predicate: (T) -> Boolean): Boolean {
    if (isEmpty()) return true
    for (i in indices) {
        if (predicate(get(i))) return false
    }
    return true
}

fun <T> List<T>.asSequenceFast(): Sequence<T> {
    return sequence {
        for (i in indices) {
            yield(get(i))
        }
    }
}

@JvmName("averageOrZeroOfByte")
fun Iterable<Byte>.averageOrZero(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count++
    }
    return if (count == 0) 0.0 else sum / count
}

/**
 * Returns an average value of elements in the collection.
 */
@JvmName("averageOrZeroOfShort")
fun Iterable<Short>.averageOrZero(): Double {
    var sum = 0.0
    var count = 0
    for (element in this) {
        sum += element
        count++
    }
    return if (count == 0) 0.0 else sum / count
}

/**
 * Returns an average value of elements in the collection.
 */
@JvmName("averageOrZeroOfInt")
fun Iterable<Int>.averageOrZero(): Double {
    var sum = 0.0
    var count = 0
    for (element in this) {
        sum += element
        count++
    }
    return if (count == 0) 0.0 else sum / count
}

/**
 * Returns an average value of elements in the collection.
 */
@JvmName("averageOrZeroOfLong")
fun Iterable<Long>.averageOrZero(): Double {
    var sum = 0.0
    var count = 0
    for (element in this) {
        sum += element
        count++
    }
    return if (count == 0) 0.0 else sum / count
}

/**
 * Returns an average value of elements in the collection.
 */
@JvmName("averageOrZeroOfFloat")
fun Iterable<Float>.averageOrZero(): Double {
    var sum = 0.0
    var count = 0
    for (element in this) {
        sum += element
        count++
    }
    return if (count == 0) 0.0 else sum / count
}

/**
 * Returns an average value of elements in the collection.
 */
@JvmName("averageOrZeroOfeDouble")
fun Iterable<Double>.averageOrZero(): Double {
    var sum = 0.0
    var count = 0
    for (element in this) {
        sum += element
        count++
    }
    return if (count == 0) 0.0 else sum / count
}

inline fun LongCollection.removeBy(predicate: (Long) -> Boolean): Boolean {
    val it = iterator()
    var changed = false
    while (it.hasNext()) {
        if (predicate(it.nextLong())) {
            it.remove()
            changed = true
        }
    }
    return changed
}