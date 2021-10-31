package cum.xiaro.trollhack.util

import cum.xiaro.trollhack.util.collections.ExtendedBitSet

class IDRegistry {
    private val bitSet = ExtendedBitSet()

    fun register(): Int {
        var id = -1

        synchronized(bitSet) {
            for (other in bitSet) {
                id = other
            }
            bitSet.add(++id)
        }

        return id
    }

    fun unregister(id: Int) {
        synchronized(bitSet) {
            bitSet.remove(id)
        }
    }
}