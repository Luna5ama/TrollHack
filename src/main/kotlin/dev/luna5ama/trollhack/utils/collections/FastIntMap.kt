package dev.luna5ama.trollhack.utils.collections

import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.objects.AbstractObjectIterator
import it.unimi.dsi.fastutil.objects.AbstractObjectSet
import it.unimi.dsi.fastutil.objects.ObjectIterator
import it.unimi.dsi.fastutil.objects.ObjectSet

@Suppress("UNCHECKED_CAST")
class FastIntMap<V : Any> : AbstractInt2ObjectMap<V>() {
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

    override fun defaultReturnValue(rv: V) {
        throw UnsupportedOperationException()
    }

    override fun defaultReturnValue(): V? {
        return null
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

    override fun getOrDefault(key: Int, defaultValue: V): V {
        return super.getOrDefault(key, defaultValue)
    }

    inline fun getOrPut(key: Int, crossinline defaultValue: () -> V): V {
        val value = get(key)
        return if (value == null) {
            val answer = defaultValue()
            put(key, answer)
            answer
        } else {
            value
        }
    }

    operator fun set(key: Int, value: V) {
        put(key, value)
    }

    override fun int2ObjectEntrySet(): ObjectSet<Int2ObjectMap.Entry<V>> {
        return object : AbstractObjectSet<Int2ObjectMap.Entry<V>>() {
            override fun iterator(): ObjectIterator<Int2ObjectMap.Entry<V>> {
                return object : AbstractObjectIterator<Int2ObjectMap.Entry<V>>() {
                    private var prevIndex = -1
                    private var index = 0

                    init {
                        skipAhead()
                    }

                    override fun hasNext(): Boolean {
                        return index < array.size
                    }

                    override fun next(): Int2ObjectMap.Entry<V> {
                        val entry = BasicEntry(index, array[index] as V)
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

                        this@FastIntMap.remove(prevIndex)
                    }
                }
            }

            override val size: Int
                get() = size0
        }
    }
}