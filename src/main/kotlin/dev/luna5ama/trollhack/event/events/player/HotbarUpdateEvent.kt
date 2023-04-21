package dev.luna5ama.trollhack.event.events.player

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting

class HotbarUpdateEvent(val oldSlot: Int, val newSlot: Int) : Event, EventPosting by Companion {
    companion object : EventBus()
}