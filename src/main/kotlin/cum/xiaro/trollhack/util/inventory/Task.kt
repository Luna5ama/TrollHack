package cum.xiaro.trollhack.util.inventory

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.manager.managers.InventoryTaskManager
import cum.xiaro.trollhack.module.AbstractModule
import cum.xiaro.trollhack.util.TimeUnit
import cum.xiaro.trollhack.util.delegate.ComputeFlag
import cum.xiaro.trollhack.util.interfaces.Helper
import cum.xiaro.trollhack.util.threads.onMainThreadSafe

inline fun SafeClientEvent.inventoryTaskNow(block: InventoryTask.Builder.() -> Unit) =
    InventoryTask.Builder()
        .apply {
            priority(Int.MAX_VALUE)
            delay(0)
        }
        .apply(block)
        .build()
        .also {
            InventoryTaskManager.runNow(this, it)
        }

inline fun AbstractModule.inventoryTask(block: InventoryTask.Builder.() -> Unit) =
    InventoryTask.Builder()
        .apply {
            priority(modulePriority)
        }.apply(block)
        .build()
        .also {
            InventoryTaskManager.addTask(it)
        }

val InventoryTask?.executedOrTrue get() = this == null || this.executed

val InventoryTask?.confirmedOrTrue get() = this == null || this.confirmed

class InventoryTask private constructor(
    private val id: Int,
    private val priority: Int,
    val delay: Long,
    val postDelay: Long,
    val timeout: Long,
    val runInGui: Boolean,
    private val clicks: Array<Step>
) : Comparable<InventoryTask>, Helper {
    val finished by ComputeFlag {
        cancelled || finishTime != -1L
    }
    val executed by ComputeFlag {
        cancelled || finished && System.currentTimeMillis() - finishTime > postDelay
    }
    val confirmed by ComputeFlag {
        cancelled || executed && futures.all { it?.timeout(timeout) ?: true }
    }

    private val futures = arrayOfNulls<StepFuture?>(clicks.size)
    private var finishTime = -1L
    private var index = 0
    private var cancelled = false

    fun runTask(event: SafeClientEvent): StepFuture? {
        if (cancelled || index >= clicks.size) {
            return null
        }

        val currentIndex = index++
        val future = clicks[currentIndex].run(event)
        futures[currentIndex] = future

        if (index >= clicks.size && finishTime == -1L) {
            onMainThreadSafe { playerController.updateController() }
            finishTime = System.currentTimeMillis()
        }

        return future
    }

    fun cancel() {
        cancelled = true
    }

    override fun compareTo(other: InventoryTask): Int {
        val result = this.priority.compareTo(other.priority)
        return if (result != 0) result
        else this.id.compareTo(other.id)
    }

    override fun equals(other: Any?) =
        this === other
            || (other is InventoryTask
            && this.id == other.id)

    override fun hashCode() = id

    class Builder {
        private val clicks = ArrayList<Step>()
        private var priority = 0
        private var delay = 25L
        private var postDelay = 50L
        private var timeout = 3000L
        private var runInGui = false

        fun priority(value: Int) {
            priority = value
        }

        fun delay(value: Int, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
            delay = value * timeUnit.multiplier
        }

        fun delay(value: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
            delay = value * timeUnit.multiplier
        }

        fun postDelay(value: Int, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
            postDelay = value * timeUnit.multiplier
        }

        fun postDelay(value: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
            postDelay = value * timeUnit.multiplier
        }

        fun timeout(value: Int, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
            timeout = value * timeUnit.multiplier
        }

        fun timeout(value: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
            timeout = value * timeUnit.multiplier
        }

        fun runInGui() {
            runInGui = true
        }

        operator fun Step.unaryPlus() {
            clicks.add(this)
        }

        fun build(): InventoryTask {
            return InventoryTask(currentID++, priority, delay, postDelay, timeout, runInGui, clicks.toTypedArray())
        }
    }

    private companion object {
        @JvmField
        var currentID = Int.MIN_VALUE
    }
}