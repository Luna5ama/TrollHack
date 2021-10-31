package cum.xiaro.trollhack.manager.managers

import cum.xiaro.trollhack.util.extension.lastValueOrNull
import cum.xiaro.trollhack.util.extension.synchronized
import cum.xiaro.trollhack.event.events.RunGameLoopEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.manager.Manager
import cum.xiaro.trollhack.module.AbstractModule
import cum.xiaro.trollhack.util.accessor.tickLength
import cum.xiaro.trollhack.util.accessor.timer
import cum.xiaro.trollhack.util.graphics.RenderUtils3D
import cum.xiaro.trollhack.util.threads.runSafe
import java.util.*
import kotlin.math.roundToInt

object TimerManager : Manager() {
    private val modifiers = TreeMap<AbstractModule, Modifier>().synchronized()
    private var modified = false

    var totalTicks = Int.MIN_VALUE
    var tickLength = 50.0f; private set

    init {
        listener<RunGameLoopEvent.Start>(Int.MAX_VALUE, true) {
            runSafe {
                synchronized(modifiers) {
                    modifiers.values.removeIf { it.endTick < totalTicks }
                    modifiers.lastValueOrNull()?.let {
                        mc.timer.tickLength = it.tickLength
                    } ?: return@runSafe null
                }

                modified = true
            } ?: run {
                modifiers.clear()
                if (modified) {
                    mc.timer.tickLength = 50.0f
                    modified = false
                }
            }

            tickLength = mc.timer.tickLength
        }

        listener<TickEvent.Pre>(Int.MAX_VALUE, true) {
            totalTicks++
        }
    }

    fun AbstractModule.resetTimer() {
        modifiers.remove(this)
    }

    fun AbstractModule.modifyTimer(tickLength: Float, timeoutTicks: Int = 1) {
        runSafe {
            modifiers[this@modifyTimer] = Modifier(tickLength, totalTicks + RenderUtils3D.partialTicks.roundToInt() + timeoutTicks)
        }
    }

    private class Modifier(
        val tickLength: Float,
        val endTick: Int
    )
}