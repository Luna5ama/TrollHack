package dev.luna5ama.trollhack.util.pause

import dev.luna5ama.trollhack.module.AbstractModule
import dev.luna5ama.trollhack.util.extension.firstEntryOrNull
import dev.luna5ama.trollhack.util.extension.synchronized
import java.util.*

abstract class PriorityTimeoutPause : ITimeoutPause {
    private val pauseMap = TreeMap<AbstractModule, Long>(Comparator.reverseOrder())

    override fun requestPause(module: AbstractModule, timeout: Long): Boolean {
        synchronized(this) {
            val flag = isOnTopPriority(module)

            if (flag) {
                pauseMap[module] = System.currentTimeMillis() + timeout
            }

            return flag
        }
    }

    fun isOnTopPriority(module: AbstractModule): Boolean {
        synchronized(this) {
            val currentTime = System.currentTimeMillis()
            var entry = pauseMap.firstEntryOrNull()

            while (entry != null && entry.key != module && (!entry.key.isActive() || entry.value < currentTime)) {
                pauseMap.pollFirstEntry()
                entry = pauseMap.firstEntry()
            }

            return entry == null
                || entry.key == module
                || entry.key.modulePriority < module.modulePriority
        }
    }

    fun getTopPriority(): AbstractModule? {
        synchronized(this) {
            val currentTime = System.currentTimeMillis()
            var entry = pauseMap.firstEntryOrNull()

            while (entry != null && (!entry.key.isActive() || entry.value < currentTime)) {
                pauseMap.pollFirstEntry()
                entry = pauseMap.firstEntry()
            }

            return entry?.key
        }
    }
}