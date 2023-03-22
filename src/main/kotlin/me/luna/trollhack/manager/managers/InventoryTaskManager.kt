package me.luna.trollhack.manager.managers

import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap
import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.event.events.ConnectionEvent
import me.luna.trollhack.event.events.PacketEvent
import me.luna.trollhack.event.events.RunGameLoopEvent
import me.luna.trollhack.event.events.WorldEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.manager.Manager
import me.luna.trollhack.util.TickTimer
import me.luna.trollhack.util.TpsCalculator
import me.luna.trollhack.util.inventory.ClickFuture
import me.luna.trollhack.util.inventory.InventoryTask
import me.luna.trollhack.util.inventory.StepFuture
import me.luna.trollhack.util.inventory.removeHoldingItem
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.network.play.server.SPacketConfirmTransaction
import java.util.*

object InventoryTaskManager : Manager() {
    private val confirmMap = Short2ObjectOpenHashMap<ClickFuture>()
    private val taskQueue = PriorityQueue<InventoryTask>()
    private val timer = TickTimer()
    private var lastTask: InventoryTask? = null

    init {
        listener<PacketEvent.Receive> {
            if (it.packet !is SPacketConfirmTransaction) return@listener
            synchronized(InventoryTaskManager) {
                confirmMap.remove(it.packet.actionNumber)?.confirm()
            }
        }

        safeListener<RunGameLoopEvent.Render> {
            if (lastTask == null && taskQueue.isEmpty()) {
                InventoryTask.resetIdCounter()
                return@safeListener
            }
            if (!timer.tick(0L)) return@safeListener

            lastTaskOrNext()?.let {
                runTask(it)
            }
        }

        listener<WorldEvent.Unload> {
            reset()
        }
    }

    fun addTask(task: InventoryTask) {
        synchronized(InventoryTaskManager) {
            taskQueue.add(task)
        }
    }

    fun runNow(event: SafeClientEvent, task: InventoryTask) {
        event {
            if (!player.inventory.itemStack.isEmpty) {
                removeHoldingItem()
            }

            while (!task.finished) {
                task.runTask(event)?.let {
                    handleFuture(it)
                }
            }

            timer.reset((task.postDelay * TpsCalculator.multiplier).toLong())
        }
    }

    private fun SafeClientEvent.lastTaskOrNext(): InventoryTask? {
        return lastTask ?: run {
            val newTask = synchronized(InventoryTaskManager) {
                taskQueue.poll()?.also { lastTask = it }
            } ?: return null

            if (!player.inventory.itemStack.isEmpty) {
                removeHoldingItem()
                return null
            }

            newTask
        }
    }

    private fun SafeClientEvent.runTask(task: InventoryTask) {
        if (mc.currentScreen is GuiContainer && !task.runInGui && !player.inventory.itemStack.isEmpty) {
            timer.reset(500L)
            return
        }

        if (task.delay == 0L) {
            runNow(this, task)
        } else {
            task.runTask(this)?.let {
                handleFuture(it)
                timer.reset((task.delay * TpsCalculator.multiplier).toLong())
            }
        }

        if (task.finished) {
            timer.reset((task.postDelay * TpsCalculator.multiplier).toLong())
            lastTask = null
            return
        }
    }

    private fun handleFuture(future: StepFuture) {
        if (future is ClickFuture) {
            synchronized(InventoryTaskManager) {
                confirmMap[future.id] = future
            }
        }
    }

    private fun reset() {
        synchronized(InventoryTaskManager) {
            confirmMap.clear()
            lastTask?.cancel()
            lastTask = null
            taskQueue.forEach {
                it.cancel()
            }
            taskQueue.clear()
        }
    }

}