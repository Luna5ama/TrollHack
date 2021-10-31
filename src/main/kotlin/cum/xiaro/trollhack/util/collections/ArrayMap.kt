package cum.xiaro.trollhack.util.collections

@Suppress("UNCHECKED_CAST")
class ArrayMap<V : Any> : MutableMap<Int, V> {
    private var array = emptyArray<Any?>()

    override val size: Int
        get() = size0

    private var size0 = 0

    override fun put(key: Int, value: V): V? {
        var prev: Any? = null

        if (key >= array.size) {
            array = array.copyOf(key + 10)
            size0++
        } else {
            prev = array[key]
            if (prev == null) size0++
        }

        array[key] = value

        return prev as V?
    }

    override fun get(key: Int): V? {
        var value: Any? = null

        if (key < array.size) {
            value = array[key]
        }

        return value as V?
    }

    override fun containsKey(key: Int): Boolean {
        return key < array.size && array[key] != null
    }

    override fun remove(key: Int): V? {
        var prev: Any? = null

        if (key < array.size) {
            prev = array[key]
            array[key] = null
            if (prev != null) size0--
        }

        return prev as V?
    }

    override fun clear() {
        size0 = 0
        array.fill(null)
    }

    override fun containsValue(value: V): Boolean {
        return array.contains(value)
    }

    override fun isEmpty(): Boolean {
        return size0 == 0
    }

    override fun putAll(from: Map<out Int, V>) {
        from.forEach {
            put(it.key, it.value)
        }
    }

    override val entries: MutableSet<MutableMap.MutableEntry<Int, V>>
        get() = object : MutableSet<MutableMap.MutableEntry<Int, V>> {
            override fun add(element: MutableMap.MutableEntry<Int, V>): Boolean {
                return put(element.key, element.value) == null
            }

            override fun addAll(elements: Collection<MutableMap.MutableEntry<Int, V>>): Boolean {
                var added = false

                elements.forEach {
                    added = add(it) || added
                }

                return added
            }

            override fun clear() {
                this@ArrayMap.clear()
            }

            override fun iterator(): MutableIterator<MutableMap.MutableEntry<Int, V>> =
                object : MutableIterator<MutableMap.MutableEntry<Int, V>> {
                    private var prevIndex = -1
                    private var index = 0

                    init {
                        skipAhead()
                    }

                    override fun hasNext(): Boolean {
                        return index < array.size
                    }

                    override fun next(): MutableMap.MutableEntry<Int, V> {
                        val entry = Entry(index)
                        prevIndex = index++
                        skipAhead()
                        return entry
                    }

                    private fun skipAhead() {
                        while (index < array.size) {
                            if (array[index] == null) {
                                index++
                            } else {
                                return
                            }
                        }

                        index++
                    }

                    override fun remove() {
                        if (prevIndex == -1) {
                            throw NoSuchElementException()
                        }

                        this@ArrayMap.remove(prevIndex)
                    }
                }

            override fun remove(element: MutableMap.MutableEntry<Int, V>): Boolean {
                return this@ArrayMap.remove(element.key) != null
            }

            override fun removeAll(elements: Collection<MutableMap.MutableEntry<Int, V>>): Boolean {
                var removed = false

                elements.forEach {
                    removed = remove(it) || removed
                }

                return removed
            }

            override fun retainAll(elements: Collection<MutableMap.MutableEntry<Int, V>>): Boolean {
                var removed = false

                for (index in array.indices) {
                    if (array[index] == null) continue
                    if (!elements.contains(Entry(index))) {
                        removed = this@ArrayMap.remove(index) != null || removed
                    }
                }

                return removed
            }

            override val size: Int
                get() = this@ArrayMap.size

            override fun contains(element: MutableMap.MutableEntry<Int, V>): Boolean {
                return array[element.key] == element.value
            }

            override fun containsAll(elements: Collection<MutableMap.MutableEntry<Int, V>>): Boolean {
                return elements.all {
                    contains(it)
                }
            }

            override fun isEmpty(): Boolean {
                return this@ArrayMap.isEmpty()
            }
        }

    override val keys: MutableSet<Int>
        get() = object : MutableSet<Int> {
            override fun add(element: Int): Boolean {
                throw UnsupportedOperationException()
            }

            override fun addAll(elements: Collection<Int>): Boolean {
                throw UnsupportedOperationException()
            }

            override fun clear() {
                this@ArrayMap.clear()
            }

            override fun iterator(): MutableIntIterator = object : MutableIntIterator {
                private var prevIndex = -1
                private var index = 0

                init {
                    skipAhead()
                }

                override fun hasNext(): Boolean {
                    return index < array.size
                }

                override fun nextInt(): Int {
                    val key = index
                    prevIndex = index++
                    skipAhead()
                    return key
                }

                private fun skipAhead() {
                    while (index < array.size) {
                        if (array[index] == null) {
                            index++
                        } else {
                            return
                        }
                    }

                    index++
                }

                override fun remove() {
                    if (prevIndex == -1) {
                        throw NoSuchElementException()
                    }

                    this@ArrayMap.remove(prevIndex)
                }
            }

            override fun remove(element: Int): Boolean {
                return this@ArrayMap.remove(element) != null
            }

            override fun removeAll(elements: Collection<Int>): Boolean {
                var removed = false

                elements.forEach {
                    removed = remove(it) || removed
                }

                return removed
            }

            override fun retainAll(elements: Collection<Int>): Boolean {
                var removed = false

                for (index in array.indices) {
                    if (array[index] == null) continue
                    if (!elements.contains(index)) {
                        removed = this@ArrayMap.remove(index) != null || removed
                    }
                }

                return removed
            }

            override val size: Int
                get() = this@ArrayMap.size

            override fun contains(element: Int): Boolean {
                return containsKey(element)
            }

            override fun containsAll(elements: Collection<Int>): Boolean {
                return elements.all {
                    containsKey(it)
                }
            }

            override fun isEmpty(): Boolean {
                return this@ArrayMap.isEmpty()
            }
        }
    override val values: MutableCollection<V>
        get() = object : MutableCollection<V> {
            override val size: Int
                get() = this@ArrayMap.size

            override fun contains(element: V): Boolean {
                return containsValue(element)
            }

            override fun containsAll(elements: Collection<V>): Boolean {
                return elements.all { containsValue(it) }
            }

            override fun isEmpty(): Boolean {
                return this@ArrayMap.isEmpty()
            }

            override fun add(element: V): Boolean {
                throw UnsupportedOperationException()
            }

            override fun addAll(elements: Collection<V>): Boolean {
                throw UnsupportedOperationException()
            }

            override fun clear() {
                this@ArrayMap.clear()
            }

            override fun iterator(): MutableIterator<V> = object : MutableIterator<V> {
                private var prevIndex = -1
                private var index = 0

                init {
                    skipAhead()
                }

                override fun hasNext(): Boolean {
                    return index < array.size
                }

                override fun next(): V {
                    val value = array[index] as V
                    prevIndex = index++
                    skipAhead()
                    return value
                }

                private fun skipAhead() {
                    while (index < array.size) {
                        if (array[index] == null) {
                            index++
                        } else {
                            return
                        }
                    }

                    index++
                }

                override fun remove() {
                    if (prevIndex == -1) {
                        throw NoSuchElementException()
                    }

                    this@ArrayMap.remove(prevIndex)
                }
            }

            override fun remove(element: V): Boolean {
                var removed = false
                val index = indexOf(element)

                if (index != - 1) {
                    removed = this@ArrayMap.remove(index) != null
                }

                return removed
            }

            override fun removeAll(elements: Collection<V>): Boolean {
                var removed = false

                elements.forEach {
                    removed = remove(it) || removed
                }

                return removed
            }

            override fun retainAll(elements: Collection<V>): Boolean {
                var removed = false

                for (index in array.indices) {
                    val element = array[index] ?: continue
                    if (!elements.contains(element)) {
                        removed = this@ArrayMap.remove(index) != null || removed
                    }
                }

                return removed
            }
        }

    private inner class Entry(override val key: Int) : MutableMap.MutableEntry<Int, V> {
        override val value: V
            get() = array[key] as V

        override fun setValue(newValue: V): V {
            return put(key, value)!!
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Map.Entry<*, *>) return false

            if (key != other.key) return false

            return true
        }

        override fun hashCode(): Int {
            return key
        }
    }
}