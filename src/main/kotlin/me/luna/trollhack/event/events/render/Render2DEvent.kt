package me.luna.trollhack.event.events.render

import me.luna.trollhack.event.Event
import me.luna.trollhack.event.EventPosting
import me.luna.trollhack.event.NamedProfilerEventBus

sealed class Render2DEvent : Event {
    object Mc : Render2DEvent(), EventPosting by NamedProfilerEventBus("mc")
    object Absolute : Render2DEvent(), EventPosting by NamedProfilerEventBus("absolute")
    object Troll : Render2DEvent(), EventPosting by NamedProfilerEventBus("troll")
}