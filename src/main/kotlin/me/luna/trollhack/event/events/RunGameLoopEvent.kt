package me.luna.trollhack.event.events

import me.luna.trollhack.event.Event
import me.luna.trollhack.event.EventPosting
import me.luna.trollhack.event.NamedProfilerEventBus

sealed class RunGameLoopEvent : Event {
    object Start : RunGameLoopEvent(), EventPosting by NamedProfilerEventBus("start")
    object Tick : RunGameLoopEvent(), EventPosting by NamedProfilerEventBus("tick")
    object Render : RunGameLoopEvent(), EventPosting by NamedProfilerEventBus("render")
    object End : RunGameLoopEvent(), EventPosting by NamedProfilerEventBus("end")
}