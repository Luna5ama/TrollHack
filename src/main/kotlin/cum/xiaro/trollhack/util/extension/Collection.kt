@file:Suppress("NOTHING_TO_INLINE")

package cum.xiaro.trollhack.util.extension

import java.util.*

inline fun <E: Any> MutableCollection<E>.add(e: E?) {
    if (e != null) this.add(e)
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the collection.
 */
inline fun <T> Iterable<T>.sumOfFloat(selector: (T) -> Float): Float {
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

inline fun <E> MutableCollection<E>.synchronized(): MutableCollection<E> =
    Collections.synchronizedCollection(this)

inline fun <E> MutableList<E>.synchronized(): MutableList<E> =
    Collections.synchronizedList(this)

inline fun <E> MutableSet<E>.synchronized(): MutableSet<E> =
    Collections.synchronizedSet(this)

inline fun <E> SortedSet<E>.synchronized(): SortedSet<E> =
    Collections.synchronizedSortedSet(this)

inline fun <E> NavigableSet<E>.synchronized(): NavigableSet<E> =
    Collections.synchronizedNavigableSet(this)