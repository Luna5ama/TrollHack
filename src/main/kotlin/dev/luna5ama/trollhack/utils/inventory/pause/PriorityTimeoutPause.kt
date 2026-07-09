package dev.luna5ama.trollhack.utils.inventory.pause

import dev.luna5ama.trollhack.modules.AbstractModule
import dev.luna5ama.trollhack.utils.extension.firstEntryOrNull
import dev.luna5ama.trollhack.utils.extension.synchronized
import java.util.*
import kotlin.Comparator

abstract class PriorityTimeoutPause : ITimeoutPause {
    private val pauseMap = TreeMap<AbstractModule, Long>(Comparator.reverseOrder()).synchronized()

    override fun requestPause(module: AbstractModule, timeout: Long): Boolean {
        synchronized(pauseMap) {
            val flag = isOnTopPriority(module)

            if (flag) {
                pauseMap[module] = System.currentTimeMillis() + timeout
            }

            return flag
        }
    }

    fun isOnTopPriority(module: AbstractModule): Boolean {
        synchronized(pauseMap) {
            val currentTime = System.currentTimeMillis()
            var entry = pauseMap.firstEntryOrNull()

            while (entry != null && entry.key != module && (!entry.key.isActive || entry.value < currentTime)) {
                pauseMap.pollFirstEntry()
                entry = pauseMap.firstEntry()
            }

            return entry == null
                || entry.key == module
                || entry.key.priority < module.priority
        }
    }

    fun getTopPriority(): AbstractModule? {
        synchronized(pauseMap) {
            val currentTime = System.currentTimeMillis()
            var entry = pauseMap.firstEntryOrNull()

            while (entry != null && (!entry.key.isActive || entry.value < currentTime)) {
                pauseMap.pollFirstEntry()
                entry = pauseMap.firstEntry()
            }

            return entry?.key
        }
    }
}