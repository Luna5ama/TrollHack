package cum.xiaro.trollhack.util.collections

abstract class AbstractBitSetIterator<E> : MutableIterator<E> {
    abstract var bit: Long

    var index = 0
    var prevIndex = -1

    final override fun hasNext(): Boolean {
        return index < 64
    }

    final override fun remove() {
        if (prevIndex == -1) {
            throw IllegalStateException()
        }

        val bitMask = 1L shl prevIndex
        bit = bit and bitMask.inv()
    }

    fun skipAhead() {
        while (index < 64) {
            val bitMask = 1L shl index
            if (bit and bitMask != 0L) break
            index++
        }
    }
}

abstract class AbstractBitSet<E> : MutableSet<E> {
    protected var bit = 0L

    protected abstract fun mapForward(input: E): Long
    protected abstract fun mapBackward(input: Long): E

    override val size: Int
        get() = bit.countOneBits()

    override fun add(element: E): Boolean {
        val mapped = mapForward(element)
        val prev = bit
        bit = bit or mapped
        return prev != bit
    }

    override fun addAll(elements: Collection<E>): Boolean {
        var mask = 0L
        for (element in elements) {
            mask = mask or mapForward(element)
        }

        val different = bit != mask
        bit = bit or mask
        return different
    }

    override fun clear() {
        bit = 0L
    }

    override fun remove(element: E): Boolean {
        val mapped = mapForward(element)
        val prev = bit
        bit = bit and mapped.inv()
        return prev != bit
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        var mask = 0L
        for (element in elements) {
            mask = mask or mapForward(element)
        }

        val different = bit != mask
        bit = bit and mask.inv()
        return different
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        var mask = 0L
        for (element in elements) {
            mask = mask or mapForward(element)
        }

        val different = bit != mask
        bit = bit and mask
        return different
    }

    override fun contains(element: E): Boolean {
        return bit and mapForward(element) != 0L
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        for (element in elements) {
            if (!contains(element)) return false
        }

        return true
    }

    override fun isEmpty(): Boolean {
        return bit == 0L
    }

    override fun iterator(): MutableIterator<E> {
        return BitSetIterator()
    }

    override fun toString(): String {
        return joinToString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbstractBitSet<*>

        if (bit != other.bit) return false

        return true
    }

    override fun hashCode(): Int {
        return bit.hashCode()
    }

    private inner class BitSetIterator : AbstractBitSetIterator<E>() {
        override var bit
            get() = this@AbstractBitSet.bit
            set(value) {
                this@AbstractBitSet.bit = value
            }

        init {
            skipAhead()
        }

        override fun next(): E {
            prevIndex = index++
            val bitMask = 1L shl prevIndex
            val result = mapBackward(bitMask)
            skipAhead()
            return result
        }
    }
}

class IntBitSet : AbstractBitSet<Int>() {
    override fun mapForward(input: Int): Long {
        return 1L shl input
    }

    override fun mapBackward(input: Long): Int {
        return input.countTrailingZeroBits()
    }

    override fun add(element: Int): Boolean {
        val mapped = mapForward(element)
        val contains = bit and mapped != 0L
        bit = bit or mapped
        return !contains
    }

    override fun remove(element: Int): Boolean {
        val mapped = mapForward(element)
        val contains = bit and mapped != 0L
        bit = bit and mapped.inv()
        return contains
    }

    override fun contains(element: Int): Boolean {
        return bit and mapForward(element) != 0L
    }

    override fun iterator(): MutableIntIterator {
        return IntBitSetIterator()
    }

    inner class IntBitSetIterator : AbstractBitSetIterator<Int>(), MutableIntIterator {
        override var bit
            get() = this@IntBitSet.bit
            set(value) {
                this@IntBitSet.bit = value
            }

        init {
            skipAhead()
        }

        override fun nextInt(): Int {
            prevIndex = index++
            val bitMask = 1L shl prevIndex
            val result = mapBackward(bitMask)
            skipAhead()
            return result
        }
    }
}