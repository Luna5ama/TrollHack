package dev.luna5ama.trollhack.event.events.baritone

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting

class BaritoneCommandEvent(val command: String) : Event, EventPosting by Companion {
    companion object : EventBus()
}