package dev.luna5ama.trollhack.event.events

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting

sealed class ConnectionEvent : Event {
    object Connect : ConnectionEvent(), EventPosting by EventBus()
    object Disconnect : ConnectionEvent(), EventPosting by EventBus()
}