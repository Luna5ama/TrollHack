@file:Suppress("unused", "UNCHECKED_CAST")

package dev.fastmc.common.sort

object ObjectIntrosort {
    fun <T : Comparable<T>> sort(array: Array<T>) {
        array.sort()
    }

    fun <T : Comparable<T>> sort(array: Array<T>, fromIndex: Int, toIndex: Int) {
        array.sort(fromIndex, toIndex)
    }

    fun <T> sort(array: Array<T>, fromIndex: Int, toIndex: Int, comparator: Comparator<in T>) {
        array.sortWith(comparator, fromIndex, toIndex)
    }
}

object ObjectInsertionSort {
    fun <T> sort(array: Array<T>, fromIndex: Int, toIndex: Int, comparator: Comparator<in T>) {
        array.sortWith(comparator, fromIndex, toIndex)
    }
}
