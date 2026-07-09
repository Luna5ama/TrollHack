package dev.luna5ama.trollhack.event.api

interface IListening {
    fun register(handler: Handler)
    fun register(handler: ParallelHandler)
    fun subscribe()
    fun unsubscribe()
}

open class ListenerOwner : IListening {
    private val listeners = ArrayList<Handler>()
    private val parallelListeners = ArrayList<ParallelHandler>()

    override fun register(handler: Handler) {
        listeners.add(handler)
    }

    override fun register(handler: ParallelHandler) {
        parallelListeners.add(handler)
    }

    override fun subscribe() {
        for (listener in listeners) {
            EventBus[listener.eventID].subscribe(listener)
        }
        for (listener in parallelListeners) {
            EventBus[listener.eventID].subscribe(listener)
        }
    }

    override fun unsubscribe() {
        for (listener in listeners) {
            EventBus[listener.eventID].unsubscribe(listener)
        }
        for (listener in parallelListeners) {
            EventBus[listener.eventID].unsubscribe(listener)
        }
    }
}

interface AlwaysListening : IListening {
    override fun register(handler: Handler) {
        EventBus[handler.eventID].subscribe(handler)
    }

    override fun register(handler: ParallelHandler) {
        EventBus[handler.eventID].subscribe(handler)
    }

    override fun subscribe() {

    }

    override fun unsubscribe() {

    }
}