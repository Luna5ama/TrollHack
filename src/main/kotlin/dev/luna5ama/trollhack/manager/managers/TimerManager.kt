package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.impl.LoopEvent
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.manager.AbstractManager
import dev.luna5ama.trollhack.modules.AbstractModule
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.utils.extension.lastValueOrNull
import dev.luna5ama.trollhack.utils.extension.synchronized
import dev.luna5ama.trollhack.utils.runSafe
import java.util.TreeMap
import kotlin.math.roundToInt

object TimerManager : AbstractManager(), AlwaysListening {
    private val modifiers = TreeMap<AbstractModule, Modifier>().synchronized()
    private var modified = false

    var globalTicks = Int.MIN_VALUE; private set
    var tickLength = 50.0f; private set

    init {
        handler<LoopEvent.Start>(Int.MAX_VALUE, true) {
            runSafe {
                synchronized(modifiers) {
                    modifiers.values.removeIf { it.endTick < globalTicks }
                    modifiers.lastValueOrNull()?.let {
                        world.tickRateManager().setTickRate(1000 / it.tickLength)
                    } ?: return@runSafe null
                }

                modified = true
            } ?: run {
                modifiers.clear()
            }

            tickLength = mc.level?.tickRateManager()?.tickrate()?.let { 1000 / it } ?: 50f
        }

        handler<TickEvent.Pre>(Int.MAX_VALUE, true) {
            globalTicks++
        }
    }

    fun AbstractModule.resetTimer() {
        modifiers.remove(this)
    }

    fun AbstractModule.modifyTimer(tickLength: Float, timeoutTicks: Int = 1) {
        runSafe {
            modifiers[this@modifyTimer] =
                Modifier(tickLength, globalTicks + mc.deltaTracker.getGameTimeDeltaPartialTick(true).roundToInt() + timeoutTicks)
        }
    }

    private class Modifier(
        val tickLength: Float,
        val endTick: Int
    )
}