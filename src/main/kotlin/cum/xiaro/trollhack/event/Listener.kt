package cum.xiaro.trollhack.event

import cum.xiaro.trollhack.util.interfaces.Nameable
import cum.xiaro.trollhack.util.ClassUtils.instance
import cum.xiaro.trollhack.util.threads.TrollHackScope
import cum.xiaro.trollhack.util.threads.runSafe
import cum.xiaro.trollhack.util.threads.runSafeSuspend
import kotlinx.coroutines.launch

const val DEFAULT_LISTENER_PRIORITY = 0

inline fun <reified E : Event> IListenerOwner.safeListener(
    noinline function: SafeClientEvent.(E) -> Unit
) = listener(this, E::class.java, DEFAULT_LISTENER_PRIORITY, false) { runSafe { function.invoke(this, it) } }

inline fun <reified E : Event> IListenerOwner.safeListener(
    priority: Int,
    noinline function: SafeClientEvent.(E) -> Unit
) = listener(this, E::class.java, priority, false) { runSafe { function.invoke(this, it) } }

inline fun <reified E : Event> IListenerOwner.safeListener(
    alwaysListening: Boolean,
    noinline function: SafeClientEvent.(E) -> Unit
) = listener(this, E::class.java, DEFAULT_LISTENER_PRIORITY, alwaysListening) { runSafe { function.invoke(this, it) } }

inline fun <reified E : Event> IListenerOwner.safeListener(
    priority: Int,
    alwaysListening: Boolean,
    noinline function: SafeClientEvent.(E) -> Unit
) = listener(this, E::class.java, priority, alwaysListening) { runSafe { function.invoke(this, it) } }


inline fun <reified E : Event> IListenerOwner.safeParallelListener(
    noinline function: suspend SafeClientEvent.(E) -> Unit
) = parallelListener(this, E::class.java, false) { runSafeSuspend { function.invoke(this, it) } }

inline fun <reified E : Event> IListenerOwner.safeParallelListener(
    alwaysListening: Boolean,
    noinline function: suspend SafeClientEvent.(E) -> Unit
) = parallelListener(this, E::class.java, alwaysListening) { runSafeSuspend { function.invoke(this, it) } }


inline fun <reified E : Event> IListenerOwner.safeConcurrentListener(
    noinline function: suspend SafeClientEvent.(E) -> Unit
) = concurrentListener(this, E::class.java, false) { runSafeSuspend { function.invoke(this, it) } }

inline fun <reified E : Event> IListenerOwner.safeConcurrentListener(
    alwaysListening: Boolean,
    noinline function: suspend SafeClientEvent.(E) -> Unit
) = concurrentListener(this, E::class.java, alwaysListening) { runSafeSuspend { function.invoke(this, it) } }


inline fun <reified E : Event> IListenerOwner.listener(
    noinline function: (E) -> Unit
) = listener(this, E::class.java, DEFAULT_LISTENER_PRIORITY, false, function)

inline fun <reified E : Event> IListenerOwner.listener(
    priority: Int,
    noinline function: (E) -> Unit
) = listener(this, E::class.java, priority, false, function)

inline fun <reified E : Event> IListenerOwner.listener(
    alwaysListening: Boolean,
    noinline function: (E) -> Unit
) = listener(this, E::class.java, DEFAULT_LISTENER_PRIORITY, alwaysListening, function)

inline fun <reified E : Event> IListenerOwner.listener(
    priority: Int,
    alwaysListening: Boolean,
    noinline function: (E) -> Unit
) = listener(this, E::class.java, priority, alwaysListening, function)


inline fun <reified E : Event> IListenerOwner.parallelListener(
    noinline function: suspend (E) -> Unit
) = parallelListener(this, E::class.java, false, function)

inline fun <reified E : Event> IListenerOwner.parallelListener(
    alwaysListening: Boolean,
    noinline function: suspend (E) -> Unit
) = parallelListener(this, E::class.java, alwaysListening, function)


inline fun <reified E : Event> IListenerOwner.concurrentListener(
    noinline function: suspend (E) -> Unit
) = concurrentListener(this, E::class.java, false, function)

inline fun <reified E : Event> IListenerOwner.concurrentListener(
    alwaysListening: Boolean,
    noinline function: suspend (E) -> Unit
) = concurrentListener(this, E::class.java, alwaysListening, function)

@Suppress("UNCHECKED_CAST")
fun <E : Event> listener(
    owner: IListenerOwner,
    eventClass: Class<E>,
    priority: Int,
    alwaysListening: Boolean,
    function: (E) -> Unit
) {
    val eventBus = getEventBus(eventClass)
    val listener = Listener(owner, eventBus.busID, priority, function as (Any) -> Unit)

    if (alwaysListening) eventBus.subscribe(listener)
    else owner.register(listener)
}

@Suppress("UNCHECKED_CAST")
fun <E : Event> parallelListener(
    owner: IListenerOwner,
    eventClass: Class<E>,
    alwaysListening: Boolean,
    function: suspend (E) -> Unit
) {
    val eventBus = getEventBus(eventClass)
    val listener = ParallelListener(owner, eventBus.busID, function as suspend (Any) -> Unit)

    if (alwaysListening) eventBus.subscribe(listener)
    else owner.register(listener)
}

@Suppress("UNCHECKED_CAST")
fun <E : Event> concurrentListener(
    owner: IListenerOwner,
    eventClass: Class<E>,
    alwaysListening: Boolean,
    function: suspend (E) -> Unit
) {
    val eventBus = getEventBus(eventClass)
    val listener = Listener(owner, eventBus.busID, Int.MAX_VALUE) { TrollHackScope.launch { function.invoke(it as E) } }

    if (alwaysListening) eventBus.subscribe(listener)
    else owner.register(listener)
}

private fun getEventBus(eventClass: Class<out Event>): EventBus {
    return try {
        eventClass.instance
    } catch (e: NoSuchFieldException) {
        eventClass.getDeclaredField("Companion")[null] as EventPosting
    }.eventBus
}

class Listener(
    owner: Any,
    eventID: Int,
    priority: Int,
    function: (Any) -> Unit
) : AbstractListener<(Any) -> Unit>(owner, eventID, priority, function)

class ParallelListener(
    owner: Any,
    eventID: Int,
    function: suspend (Any) -> Unit
) : AbstractListener<suspend (Any) -> Unit>(owner, eventID, DEFAULT_LISTENER_PRIORITY, function)

sealed class AbstractListener<F>(
    owner: Any,
    val eventID: Int,
    val priority: Int,
    val function: F
) {
    val ownerName: String = if (owner is Nameable) owner.nameAsString else owner.javaClass.simpleName
}