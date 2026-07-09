@file:Suppress("unused")

package dev.fastmc.common.collection

typealias FastObjectArrayList<E> = dev.luna5ama.trollhack.utils.collections.FastObjectArrayList<E>

inline fun <T, reified R> Array<T>.mapArray(transform: (T) -> R): Array<R> {
    return Array(size) { transform(this[it]) }
}
