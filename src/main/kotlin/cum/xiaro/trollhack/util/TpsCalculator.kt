package cum.xiaro.trollhack.util

import cum.xiaro.trollhack.util.collections.CircularArray
import cum.xiaro.trollhack.util.collections.CircularArray.Companion.average
import cum.xiaro.trollhack.event.AlwaysListening
import cum.xiaro.trollhack.event.events.ConnectionEvent
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.listener
import net.minecraft.network.play.server.SPacketTimeUpdate

object TpsCalculator : AlwaysListening {
    // Circular Buffer lasting ~60 seconds for tick storage
    private val tickRates = CircularArray(120, 20.0f)

    private var timeLastTimeUpdate = -1L

    val tickRate: Float
        get() = tickRates.average()

    val adjustTicks: Float
        get() = tickRates.average() - 20.0f

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