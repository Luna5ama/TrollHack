package dev.luna5ama.trollhack.event.events.render

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting

class ResolutionUpdateEvent(val width: Int, val height: Int) : Event, EventPosting by Companion {
    companion object : EventBus()
}