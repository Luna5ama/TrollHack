package me.luna.trollhack.event.events

import me.luna.trollhack.event.Event
import me.luna.trollhack.event.EventPosting
import me.luna.trollhack.event.NamedProfilerEventBus

sealed class ProcessKeyBindEvent : Event {
    object Pre : ProcessKeyBindEvent(), EventPosting by NamedProfilerEventBus("pre")
    object Post : ProcessKeyBindEvent(), EventPosting by NamedProfilerEventBus("post")
}