package cum.xiaro.trollhack.util.threads

import cum.xiaro.trollhack.event.AlwaysListening
import cum.xiaro.trollhack.event.events.RunGameLoopEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.util.Wrapper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object MainThreadExecutor : AlwaysListening {
    private val jobs = ArrayList<MainThreadJob<*>>()
    private val mutex = Mutex()

    init {
        listener<RunGameLoopEvent.Start>(Int.MAX_VALUE, true) {
            runJobs()
        }
        listener<RunGameLoopEvent.Tick>(Int.MAX_VALUE, true) {
            runJobs()
        }
        listener<RunGameLoopEvent.Render>(Int.MAX_VALUE, true) {
            runJobs()
        }
        listener<RunGameLoopEvent.End>(Int.MAX_VALUE, true) {
            runJobs()
        }
    }

    private fun runJobs() {
        if (jobs.isEmpty()) return

        runBlocking {
            mutex.withLock {
                jobs.forEach {
                    it.run()
                }
                jobs.clear()
            }
        }
    }

    fun <T> add(block: () -> T) =
        MainThreadJob(block).apply {
            if (Wrapper.minecraft.isCallingFromMinecraftThread) {
                run()
            } else {
                runBlocking {
                    mutex.withLock {
                        jobs.add(this@apply)
                    }
                }
            }
        }.deferred

    suspend fun <T> addSuspend(block: () -> T) =
        MainThreadJob(block).apply {
            if (Wrapper.minecraft.isCallingFromMinecraftThread) {
                run()
            } else {
                mutex.withLock {
                    jobs.add(this)
                }
            }
        }.deferred

    private class MainThreadJob<T>(private val block: () -> T) {
        val deferred = CompletableDeferred<T>()

        fun run() {
            deferred.completeWith(
                runCatching { block.invoke() }
            )
        }
    }
}