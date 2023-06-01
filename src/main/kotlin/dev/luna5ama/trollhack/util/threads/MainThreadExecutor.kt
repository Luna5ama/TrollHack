package dev.luna5ama.trollhack.util.threads

import dev.luna5ama.trollhack.event.AlwaysListening
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.util.Wrapper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object MainThreadExecutor : AlwaysListening {
    private var jobs = ArrayList<MainThreadJob<*>>()
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
        if (jobs.isNotEmpty()) {
            runBlocking {
                val prev: List<MainThreadJob<*>>

                mutex.withLock {
                    prev = jobs
                    jobs = ArrayList()
                }

                prev.forEach {
                    it.run()
                }
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