package dev.luna5ama.trollhack.event.events

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventPosting
import dev.luna5ama.trollhack.event.NamedProfilerEventBus

sealed class ProcessKeyBindEvent : Event {
    object Pre : ProcessKeyBindEvent(), EventPosting by NamedProfilerEventBus("pre")
    object Post : ProcessKeyBindEvent(), EventPosting by NamedProfilerEventBus("post")
}