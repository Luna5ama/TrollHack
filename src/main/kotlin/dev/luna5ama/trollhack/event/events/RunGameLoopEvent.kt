package dev.luna5ama.trollhack.event.events

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventPosting
import dev.luna5ama.trollhack.event.NamedProfilerEventBus

sealed class RunGameLoopEvent : Event {
    object Start : RunGameLoopEvent(), EventPosting by NamedProfilerEventBus("start")
    object Tick : RunGameLoopEvent(), EventPosting by NamedProfilerEventBus("tick")
    object Render : RunGameLoopEvent(), EventPosting by NamedProfilerEventBus("render")
    object End : RunGameLoopEvent(), EventPosting by NamedProfilerEventBus("end")
}