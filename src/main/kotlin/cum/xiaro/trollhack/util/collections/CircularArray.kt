package cum.xiaro.trollhack.util.collections

class CircularArray<E> private constructor(private val array: Array<Any?>, filled: Boolean) : MutableList<E> {
    constructor(size: Int) : this(arrayOfNulls(size), false)

    constructor(size: Int, defaultValue: E) : this(Array(size) { defaultValue }, true)

    constructor(size: Int, init: (Int) -> E) : this(Array(size, init), true)

    override var size = if (filled) array.size else 0; private set
    private var index = 0

    override fun isEmpty(): Boolean {
        return size == 0
    }

    @Suppress("UNCHECKED_CAST")
    override fun get(index: Int): E {
        checkIndex(index)
        return array[index] as E
    }

    override fun indexOf(element: E): Int {
        for (i in 0 until size) {
            if (array[i] == element) return i
        }

        return -1
    }

    override fun lastIndexOf(element: E): Int {
        for (i in size - 1 downTo 0) {
            if (array[i] == element) return i
        }

        return -1
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(elements: Collection<E>): Boolean {
        for (element in elements) {
            add(element)
        }

        return true
    }

    override fun add(index: Int, element: E) {
        checkIndexArray(index)

        if (size < array.size) {
            when (index) {
                0 -> {
                    if (isEmpty()) {
                        add(element)
                    } else {
                        moveBackward(index)
                        add0(index, element)
                    }
                }
                size -> {
                    add0(index, element)
                }
                else -> {
                    moveBackward(index)
                    add0(index, element)
                }
            }
        } else {
            if (index > 0) {
                move(0, 1, index - 1)
            }

            val lastIndex = size - 1
            val end = lastIndex - index
            if (end > 0) {
                array[0] = array[lastIndex]
                move(index, index + 1, end)
            }

            array[index] = element
        }
    }

    override fun add(element: E): Boolean {
        add0(index, element)
        return true
    }

    private fun add0(index: Int, element: E) {
        array[index] = element
        this.index = (this.index + 1) % array.size
        if (size < array.size) size++
    }

    @Suppress("UNCHECKED_CAST")
    override fun set(index: Int, element: E): E {
        checkIndex(index)
        val prev = array[index]
        array[index] = element
        return prev as E
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        var removed = false

        removeIf { element ->
            !elements.contains(element).also { removed = removed || it }
        }

        return removed
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        var removed = false

        for (element in elements) {
            removed = remove(element) || removed
        }

        return removed
    }

    override fun remove(element: E): Boolean {
        for (i in 0 until size) {
            if (array[i] == element) removeAt0(i)
            return true
        }

        return false
    }

    override fun removeAt(index: Int): E {
        checkIndex(index)
        return removeAt0(index)
    }

    private fun removeAt0(index: Int): E {
        val element = get(index)

        val numMoved = size - index - 1
        if (numMoved > 0) {
            move(index + 1, index, numMoved)
        }

        array[size - 1] = null
        size--
        return element
    }

    override fun clear() {
        this.size = 0
        this.index = 0
    }

    override fun contains(element: E): Boolean {
        return array.contains(element)
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        return elements.all(array::contains)
    }

    @Suppress("UNCHECKED_CAST")
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        checkIndex(fromIndex)
        checkIndex(toIndex)

        return ArrayList<E>(toIndex - fromIndex).apply {
            for (i in fromIndex until toIndex) {
                add(array[i] as E)
            }
        }
    }

    override fun iterator(): MutableIterator<E> = object : MutableIterator<E> {
        private var index = 0

        override fun hasNext(): Boolean = index in 0 until size

        override fun next(): E {
            if (index >= size) {
                throw NoSuchElementException("Index: $index")
            }

            return get(index++)
        }

        override fun remove() {
            removeAt(index-- - 1)
        }
    }

    override fun listIterator() = listIterator(0)

    override fun listIterator(index: Int) = object : MutableListIterator<E> {
        private var index0 = index

        override fun hasPrevious() = index0 in 0 until size

        override fun hasNext() = index0 in 0 until size

        override fun previousIndex() = index0

        override fun nextIndex() = index0

        override fun previous(): E {
            if (index0 < 0 || index0 >= size) {
                throw NoSuchElementException("Index: $index0")
            }

            return get(index0--)
        }

        override fun next(): E {
            if (index0 < 0 || index0 >= size) {
                throw NoSuchElementException("Index: $index0")
            }

            return get(index0++)
        }

        override fun add(element: E) {
            add(index0, element)
        }

        override fun remove() {
            removeAt(index0-- - 1)
        }

        override fun set(element: E) {
            set(index, element)
        }
    }

    override fun toString(): String {
        return StringBuilder().run {
            val lastIndex = size - 1
            append('[')

            for (i in 0..lastIndex) {
                append(array[i])
                if (i != lastIndex) append(", ")
            }

            append(']')
            toString()
        }
    }

    private fun checkIndexArray(index: Int) {
        if (index >= array.size || index > size) {
            throw IndexOutOfBoundsException("$index")
        }
    }

    private fun checkIndex(index: Int) {
        if (size == 0 || index < 0 || index >= size) {
            throw IndexOutOfBoundsException("$index")
        }
    }

    private fun moveBackwardFull(index: Int) {
        move(index, index + 1, size - index - 1)
    }

    private fun moveBackward(index: Int) {
        move(index, index + 1, size - index)
    }

    private fun move(from: Int, to: Int, size: Int) {
        System.arraycopy(array, from, array, to, size)
    }

    companion object {
        fun CircularArray<Float>.average(): Float {
            if (size == 0) return 0.0f

            var sum = 0.0f
            for (i in 0 until size) {
                sum += this[i]
            }
            return sum / size
        }

        fun CircularArray<Int>.average(): Double {
            if (size == 0) return 0.0

            var sum = 0.0
            for (i in 0 until size) {
                sum += this[i]
            }
            return sum / size
        }
    }
}