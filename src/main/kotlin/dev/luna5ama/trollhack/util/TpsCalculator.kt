package dev.luna5ama.trollhack.util

import dev.luna5ama.trollhack.event.AlwaysListening
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.listener
import dev.fastmc.common.collection.CircularArray
import dev.fastmc.common.collection.CircularArray.Companion.average
import net.minecraft.network.play.server.SPacketTimeUpdate
import kotlin.math.max

object TpsCalculator : AlwaysListening {
    // Circular Buffer lasting ~60 seconds for tick storage
    private val tickRates = CircularArray(120, 20.0f)

    private var timeLastTimeUpdate = -1L

    val tickRate: Float
        get() = max(tickRates.average(), 1.0f)

    val multiplier: Float
        get() = 20.0f / tickRate

    init {
        listener<PacketEvent.Receive> {
            if (it.packet !is SPacketTimeUpdate) return@listener

            if (timeLastTimeUpdate != -1L) {
                val timeElapsed = (System.nanoTime() - timeLastTimeUpdate) / 1E9
                tickRates.add((20.0 / timeElapsed).coerceIn(0.0, 20.0).toFloat())
            }

            timeLastTimeUpdate = System.nanoTime()
        }

        listener<ConnectionEvent.Connect> {
            reset()
        }
    }

    private fun reset() {
        tickRates.clear()
        timeLastTimeUpdate = -1L
    }
}