package dev.luna5ama.trollhack.event.events.render

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventPosting
import dev.luna5ama.trollhack.event.NamedProfilerEventBus

sealed class Render2DEvent : Event {
    object Mc : Render2DEvent(), EventPosting by NamedProfilerEventBus("mc")
    object Absolute : Render2DEvent(), EventPosting by NamedProfilerEventBus("absolute")
    object Troll : Render2DEvent(), EventPosting by NamedProfilerEventBus("troll")
}