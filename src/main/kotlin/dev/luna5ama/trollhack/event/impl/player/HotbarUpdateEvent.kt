package dev.luna5ama.trollhack.event.impl.player

import dev.luna5ama.trollhack.event.api.EventBus
import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.event.api.IPosting

class HotbarUpdateEvent(val oldSlot: Int, val newSlot: Int) : IEvent, IPosting by Companion {
    companion object : EventBus()
}