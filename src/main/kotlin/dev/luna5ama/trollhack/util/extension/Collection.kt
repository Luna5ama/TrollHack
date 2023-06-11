package dev.luna5ama.trollhack.util.extension

import it.unimi.dsi.fastutil.longs.LongCollection
import java.util.*

fun <E : Any> MutableCollection<E>.add(e: E?) {
    if (e != null) this.add(e)
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the collection.
 */
fun <T> Iterable<T>.sumOfFloat(selector: (T) -> Float): Float {
    var sum = 0.0f
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

inline fun <T> Sequence<T>.sumOfFloat(selector: (T) -> Float): Float {
    var sum = 0.0f
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

inline fun CharSequence.sumOfFloat(selector: (Char) -> Float): Float {
    var sum = 0.0f

    for (element in this) {
        sum += selector(element)
    }

    return sum
}

fun <E> MutableCollection<E>.synchronized(): MutableCollection<E> =
    Collections.synchronizedCollection(this)

fun <E> MutableList<E>.synchronized(): MutableList<E> =
    Collections.synchronizedList(this)

fun <E> MutableSet<E>.synchronized(): MutableSet<E> =
    Collections.synchronizedSet(this)

fun <E> SortedSet<E>.synchronized(): SortedSet<E> =
    Collections.synchronizedSortedSet(this)

fun <E> NavigableSet<E>.synchronized(): NavigableSet<E> =
    Collections.synchronizedNavigableSet(this)

inline fun LongCollection.forEach(action: (Long) -> Unit) {
    val it = this.iterator()
    while (it.hasNext()) {
        action(it.nextLong())
    }
}