package me.luna.trollhack.util.threads

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.luna.trollhack.TrollHackMod
import me.luna.trollhack.event.AlwaysListening
import me.luna.trollhack.event.events.RunGameLoopEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.util.Wrapper
import net.minecraft.network.INetHandler
import net.minecraft.network.Packet

object MainThreadExecutor : AlwaysListening {
    private var jobs = ArrayList<MainThreadJob<*>>()
    private var packets = ArrayList<Any>()
    private val lock = Any()
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

        if (packets.isNotEmpty()) {
            val prev: List<Any>

            synchronized(lock) {
                prev = packets
                packets = ArrayList()
            }

            val iterator = prev.iterator()

            while (iterator.hasNext()) {
                @Suppress("UNCHECKED_CAST")
                val packet = iterator.next() as Packet<in INetHandler>
                val processor = iterator.next() as INetHandler

                try {
                    packet.processPacket(processor)
                } catch (exception: RuntimeException) {
                    TrollHackMod.logger.fatal("Error processing packet", exception);
                }
            }
        }
    }

    fun addProcessingPacket(packetIn: Packet<*>, processor: INetHandler) {
        synchronized(lock) {
            packets.add(packetIn)
            packets.add(processor)
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