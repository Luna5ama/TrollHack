package cum.xiaro.trollhack.util.collections

import java.util.*

class ExtendedBitSet : MutableSet<Int> {
    private var bitArray = EMPTY_LONG_ARRAY

    override val size: Int
        get() {
            var sum = 0
            for (bit in bitArray) {
                sum += bit.countOneBits()
            }
            return sum
        }

    override fun add(element: Int): Boolean {
        val bit = 1L shl (element and 0x3F)
        val index = element shr 6

        val prev = getBit(index)
        val result = prev or bit
        putBit(index, result)

        return result != prev
    }

    override fun addAll(elements: Collection<Int>): Boolean {
        var modified = false

        for (element in elements) {
            modified = add(element) || modified
        }

        return modified
    }

    override fun clear() {
        bitArray = EMPTY_LONG_ARRAY
    }

    override fun remove(element: Int): Boolean {
        val bit = 1L shl (element and 0x3F)
        val index = element shr 6

        val prev = getBit(index)
        val result = bit and bit
        bitArray[index] = result

        return result != prev
    }

    override fun removeAll(elements: Collection<Int>): Boolean {
        var modified = false

        for (element in elements) {
            modified = remove(element) || modified
        }

        return modified
    }

    override fun retainAll(elements: Collection<Int>): Boolean {
        val newArray = LongArray(bitArray.size)
        for (element in elements) {
            val index = element shr 6
            if (index >= newArray.size) continue

            val bit = 1L shl (element and 0x3F)
            val prev = newArray[index]
            newArray[index] = prev or bit
        }

        var modified = false

        for ((i, bit) in bitArray.withIndex()) {
            val mask = newArray[i]
            val result = mask and bit
            bitArray[i] = result
            modified = modified || result != bit
        }

        return modified
    }

    override fun contains(element: Int): Boolean {
        val index = element shr 6
        if (index >= bitArray.size) return false

        val bit = 1L shl (element and 0x3F)
        return bitArray[index] and bit != 0L
    }

    override fun containsAll(elements: Collection<Int>): Boolean {
        for (element in elements) {
            if (!contains(element)) return false
        }

        return true
    }

    override fun isEmpty(): Boolean {
        for (bit in bitArray) {
            if (bit != 0L) return false
        }

        return true
    }

    private fun getBit(index: Int): Long {
        return if (index >= bitArray.size) {
            0L
        } else {
            bitArray[index]
        }
    }

    private fun putBit(index: Int, bit: Long) {
        if (index >= bitArray.size) {
            @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
            bitArray = Arrays.copyOf(bitArray, index + 2)
        }

        bitArray[index] = bit
    }

    override fun iterator(): MutableIntIterator {
        return ExtendedBitSetIterator()
    }

    override fun toString(): String {
        return joinToString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExtendedBitSet

        return bitArray.contentEquals(other.bitArray)
    }

    override fun hashCode(): Int {
        return bitArray.contentHashCode()
    }

    private inner class ExtendedBitSetIterator : MutableIntIterator {
        private var arrayIndex = 0
        private var bitIndex = 0

        private var prevArrayIndex = -1
        private var prevBitIndex = -1

        init {
            skipAhead()
        }

        override fun nextInt(): Int {
            prevArrayIndex = arrayIndex
            prevBitIndex = bitIndex++

            if (bitIndex >= 64) {
                arrayIndex++
                bitIndex = 0
            }

            val result = (prevArrayIndex shl 6) + prevBitIndex
            skipAhead()
            return result
        }

        override fun hasNext(): Boolean {
            return arrayIndex < bitArray.size
        }

        override fun remove() {
            if (prevBitIndex == -1) {
                throw IllegalStateException()
            }

            val bitMask = 1L shl prevBitIndex
            bitArray[prevArrayIndex] = bitArray[prevArrayIndex] and bitMask.inv()
        }

        fun skipAhead() {
            loop@ while (arrayIndex < bitArray.size) {
                val bits = bitArray[arrayIndex]

                while (bitIndex < 64) {
                    val bitMask = 1L shl bitIndex
                    if (bits and bitMask != 0L) break@loop
                    bitIndex++
                }

                arrayIndex++
                bitIndex = 0
            }
        }
    }

    companion object {
        @JvmStatic
        private val EMPTY_LONG_ARRAY = longArrayOf()
    }
}