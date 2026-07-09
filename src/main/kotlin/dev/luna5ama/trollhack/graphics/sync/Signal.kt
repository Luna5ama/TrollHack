package dev.luna5ama.trollhack.graphics.sync

import dev.luna5ama.trollhack.utils.extension.getValue
import dev.luna5ama.trollhack.utils.extension.setValue
import java.util.concurrent.atomic.AtomicBoolean

class Signal {
    private var signaled by AtomicBoolean(false)

    fun trigger() {
        signaled = true
    }

    fun check(): Boolean {
        val state = signaled
        if (state) signaled = false
        return state
    }
}