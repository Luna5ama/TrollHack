package dev.luna5ama.trollhack.event.api

import kotlinx.coroutines.launch
import dev.luna5ama.trollhack.utils.*
import dev.luna5ama.trollhack.utils.threads.RenderThreadCoroutine

const val DEFAULT_LISTENER_PRIORITY: Int = 0

inline fun <reified E : IEvent> IListening.nonNullHandler(
    noinline function: NonNullContext.(E) -> Unit
) = handler(this, E::class.java, DEFAULT_LISTENER_PRIORITY, false) { runSafe { function.invoke(this, it) } }

inline fun <reified E : IEvent> IListening.nonNullHandler(
    priority: Int,
    noinline function: NonNullContext.(E) -> Unit
) = handler(this, E::class.java, priority, false) { runSafe { function.invoke(this, it) } }

inline fun <reified E : IEvent> IListening.nonNullHandler(
    alwaysListening: Boolean,
    noinline function: NonNullContext.(E) -> Unit
) = handler(this, E::class.java, DEFAULT_LISTENER_PRIORITY, alwaysListening) { runSafe { function.invoke(this, it) } }

inline fun <reified E : IEvent> IListening.nonNullHandler(
    priority: Int,
    alwaysListening: Boolean,
    noinline function: NonNullContext.(E) -> Unit
) = handler(this, E::class.java, priority, alwaysListening) { runSafe { function.invoke(this, it) } }


inline fun <reified E : IEvent> IListening.nonNullParallelHandler(
    noinline function: suspend NonNullContext.(E) -> Unit
) = parallelHandler(this, E::class.java, false) { runSafeSuspend { function.invoke(this, it) } }

inline fun <reified E : IEvent> IListening.nonNullParallelHandler(
    alwaysListening: Boolean,
    noinline function: suspend NonNullContext.(E) -> Unit
) = parallelHandler(this, E::class.java, alwaysListening) { runSafeSuspend { function.invoke(this, it) } }


inline fun <reified E : IEvent> IListening.nonNullConcurrentHandler(
    noinline function: suspend NonNullContext.(E) -> Unit
) = concurrentHandler(this, E::class.java, false) { runSafeSuspend { function.invoke(this, it) } }

inline fun <reified E : IEvent> IListening.nonNullConcurrentHandler(
    alwaysListening: Boolean,
    noinline function: suspend NonNullContext.(E) -> Unit
) = concurrentHandler(this, E::class.java, alwaysListening) { runSafeSuspend { function.invoke(this, it) } }


inline fun <reified E : IEvent> IListening.handler(
    noinline function: (E) -> Unit
) = handler(this, E::class.java, DEFAULT_LISTENER_PRIORITY, false, function)

inline fun <reified E : IEvent> IListening.handler(
    priority: Int,
    noinline function: (E) -> Unit
) = handler(this, E::class.java, priority, false, function)

inline fun <reified E : IEvent> IListening.handler(
    alwaysListening: Boolean,
    noinline function: (E) -> Unit
) = handler(this, E::class.java, DEFAULT_LISTENER_PRIORITY, alwaysListening, function)

inline fun <reified E : IEvent> IListening.handler(
    priority: Int,
    alwaysListening: Boolean,
    noinline function: (E) -> Unit
) = handler(this, E::class.java, priority, alwaysListening, function)


inline fun <reified E : IEvent> IListening.parallelHandler(
    noinline function: suspend (E) -> Unit
) = parallelHandler(this, E::class.java, false, function)

inline fun <reified E : IEvent> IListening.parallelHandler(
    alwaysListening: Boolean,
    noinline function: suspend (E) -> Unit
) = parallelHandler(this, E::class.java, alwaysListening, function)


inline fun <reified E : IEvent> IListening.concurrentHandler(
    noinline function: suspend (E) -> Unit
) = concurrentHandler(this, E::class.java, false, function)

inline fun <reified E : IEvent> IListening.concurrentHandler(
    alwaysListening: Boolean,
    noinline function: suspend (E) -> Unit
) = concurrentHandler(this, E::class.java, alwaysListening, function)

@JvmOverloads
fun <E : IEvent> IListening.nonNullHandler(
    clz: Class<out E>,
    priority: Int = DEFAULT_LISTENER_PRIORITY,
    alwaysListening: Boolean = false,
    function: NonNullContext.(E) -> Unit
) = handler(this, clz, priority, alwaysListening) { runSafe { function.invoke(this, it) } }

@JvmOverloads
fun <E : IEvent> IListening.nonNullParallelHandler(
    clz: Class<out E>,
    alwaysListening: Boolean = false,
    function: suspend NonNullContext.(E) -> Unit
) = parallelHandler(this, clz, alwaysListening) { runSafeSuspend { function.invoke(this, it) } }
@JvmOverloads
fun <E : IEvent> IListening.nonNullConcurrentHandler(
    clz: Class<out E>,
    alwaysListening: Boolean = false,
    function: suspend NonNullContext.(E) -> Unit
) = concurrentHandler(this, clz, alwaysListening) { runSafeSuspend { function.invoke(this, it) } }

@Suppress("UNCHECKED_CAST")
fun <E : IEvent> handler(
    owner: IListening,
    eventClass: Class<E>,
    priority: Int,
    alwaysListening: Boolean,
    function: (E) -> Unit
) {
    val eventBus = getEventBus(eventClass)
    val handler = Handler(owner, eventBus.busID, priority, function as (Any) -> Unit)

    if (alwaysListening) eventBus.subscribe(handler)
    else owner.register(handler)
}

@Suppress("UNCHECKED_CAST")
fun <E : IEvent> parallelHandler(
    owner: IListening,
    eventClass: Class<E>,
    alwaysListening: Boolean,
    function: suspend (E) -> Unit
) {
    val eventBus = getEventBus(eventClass)
    val handler = ParallelHandler(owner, eventBus.busID, function as suspend (Any) -> Unit)

    if (alwaysListening) eventBus.subscribe(handler)
    else owner.register(handler)
}

@Suppress("UNCHECKED_CAST")
fun <E : IEvent> concurrentHandler(
    owner: IListening,
    eventClass: Class<E>,
    alwaysListening: Boolean,
    function: suspend (E) -> Unit
) {
    val eventBus = getEventBus(eventClass)
    val handler =
        Handler(owner, eventBus.busID, Int.MAX_VALUE) { RenderThreadCoroutine.launch { function.invoke(it as E) } }

    if (alwaysListening) eventBus.subscribe(handler)
    else owner.register(handler)
}

private fun getEventBus(eventClass: Class<out IEvent>): EventBus {
    return (eventClass.instance ?: eventClass.getDeclaredField("Companion")[null] as IPosting).eventBus
}

class Handler(
    owner: Any,
    eventID: Int,
    priority: Int,
    function: (Any) -> Unit
) : AbstractHandler<(Any) -> Unit>(owner, eventID, priority, function)

class ParallelHandler(
    owner: Any,
    eventID: Int,
    function: suspend (Any) -> Unit
) : AbstractHandler<suspend (Any) -> Unit>(owner, eventID, DEFAULT_LISTENER_PRIORITY, function)

sealed class AbstractHandler<F>(
    val owner: Any,
    val eventID: Int,
    val priority: Int,
    val function: F
) {
    val ownerName: String = if (owner is Nameable) owner.nameAsString else owner.javaClass.simpleName
}