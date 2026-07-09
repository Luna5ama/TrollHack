package dev.luna5ama.trollhack.event.impl

import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.event.api.IPosting
import dev.luna5ama.trollhack.event.api.NamedProfilerEventBus

sealed class TickEvent : IEvent {
    internal object Pre : TickEvent(), IPosting by NamedProfilerEventBus("NullTickPre")
    internal object Post : TickEvent(), IPosting by NamedProfilerEventBus("NullTickPost")
}