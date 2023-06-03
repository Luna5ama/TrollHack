package dev.luna5ama.trollhack.util.threads

import dev.fastmc.common.DoubleBuffered
import dev.fastmc.common.collection.FastObjectArrayList
import dev.luna5ama.trollhack.event.AlwaysListening
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.util.Wrapper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.completeWith

object MainThreadExecutor : AlwaysListening {
    private val jobs = DoubleBuffered { FastObjectArrayList<MainThreadJob<*>>() }
    private val signal = Channel<Unit>(1, BufferOverflow.DROP_OLDEST)

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
        signal.tryReceive()
        jobs.swap().front.runSynchronized {
            forEach {
                it.run()
            }
            clear()
        }
    }

    suspend fun runJobAdapter() {
        signal.receive()
        jobs.swap().front.runSynchronized {
            forEach {
                it.run()
            }
            clear()
        }
    }

    fun <T> add(block: () -> T): CompletableDeferred<T> {
        val job = MainThreadJob(block)
        if (Wrapper.minecraft.isCallingFromMinecraftThread) {
            job.run()
        } else {
            jobs.back.runSynchronized {
                add(job)
            }
            signal.trySend(Unit)
        }
        return job.deferred
    }

    private class MainThreadJob<T>(private val block: () -> T) {
        val deferred = CompletableDeferred<T>()

        fun run() {
            deferred.completeWith(
                runCatching { block.invoke() }
            )
        }
    }
}