package dev.luna5ama.trollhack.event.events

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventPosting
import dev.luna5ama.trollhack.event.NamedProfilerEventBus

sealed class TickEvent : Event {
    object Pre : TickEvent(), EventPosting by NamedProfilerEventBus("trollTickPre")
    object Post : TickEvent(), EventPosting by NamedProfilerEventBus("trollTickPost")
}