package dev.luna5ama.trollhack.event.api

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import dev.luna5ama.trollhack.utils.Helper
import dev.luna5ama.trollhack.utils.collections.ArrayMap
import dev.luna5ama.trollhack.utils.threads.Coroutine
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

open class NamedProfilerEventBus(private val profilerName: String) : ProfilerEventBus() {
    override fun post(event: Any) {
        TrollHackMod.profiler(profilerName) {
            super.post(event)
        }
    }
}

open class ProfilerEventBus : EventBus(), Helper {
    override fun post(event: Any) {
        val profiler = TrollHackMod.profiler
        profiler("serial/concurrent") {
            for (listener in handlers) {
                profiler(listener.ownerName) {
                    runCatching {
                        listener.function.invoke(event)
                    }.onFailure { e ->
                        TrollHackMod.LOGGER.error("Error while posting event $event", e)
                        e.printStackTrace()
                    }
                }
            }
        }
        profiler("parallel") {
            invokeParallel(event)
        }
    }
}

open class EventBus : IPosting {
    override val eventBus: EventBus
        get() = this
    val busID = id.getAndIncrement()

    init {
        eventBusMap[busID] = this
    }

    protected val handlers = CopyOnWriteArrayList<Handler>()
    private val parallelHandlers = CopyOnWriteArrayList<ParallelHandler>()

    override fun post(event: Any) {
        for (listener in handlers) {
            runCatching {
                listener.function.invoke(event)
            }.onFailure { e ->
                TrollHackMod.LOGGER.error("Error while posting event $event", e)
                e.printStackTrace()
            }
        }

        invokeParallel(event)
    }

    protected fun invokeParallel(event: Any) {
        if (!parallelHandlers.isEmpty()) {
            try {
                runBlocking(Coroutine.context) {
                    for (listener in parallelHandlers) {
                        launch {
                            runCatching {
                                listener.function.invoke(event)
                            }.onFailure { e ->
                                TrollHackMod.LOGGER.error("Error while posting event $event", e)
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } catch (ce: CancellationException) {
                ce.printStackTrace()
            }
        }
    }

    fun subscribe(handler: Handler) {
        for (i in handlers.indices) {
            val other = handlers[i]
            if (handler == other) {
                return
            } else if (handler.priority > other.priority) {
                handlers.add(i, handler)
                return
            }
        }

        handlers.add(handler)
    }

    fun subscribe(listener: ParallelHandler) {
        for (i in parallelHandlers.indices) {
            val other = parallelHandlers[i]
            if (listener == other) {
                return
            } else if (listener.priority > other.priority) {
                parallelHandlers.add(i, listener)
                return
            }
        }

        parallelHandlers.add(listener)
    }

    fun unsubscribe(handler: Handler) {
        handlers.removeIf {
            it == handler
        }
    }

    fun unsubscribe(listener: ParallelHandler) {
        parallelHandlers.removeIf {
            it == listener
        }
    }

    companion object {
        private val id = AtomicInteger()
        private val eventBusMap = ArrayMap<EventBus>()

        operator fun get(busID: Int): EventBus {
            return eventBusMap[busID]!!
        }
    }
}