package dev.luna5ama.trollhack.util.collections


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

fun <T> List<T>.asSequenceFast(): Sequence<T> {
    return sequence {
        for (i in indices) {
            yield(get(i))
        }
    }
}