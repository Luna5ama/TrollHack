package me.luna.trollhack.event.events.player

import me.luna.trollhack.event.Event
import me.luna.trollhack.event.EventBus
import me.luna.trollhack.event.EventPosting

class HotbarUpdateEvent(val oldSlot: Int, val newSlot: Int) : Event, EventPosting by Companion {
    companion object : EventBus()
}