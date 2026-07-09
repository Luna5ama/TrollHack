package dev.luna5ama.trollhack.event.impl

import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.event.api.IPosting
import dev.luna5ama.trollhack.event.api.NamedProfilerEventBus

sealed class LoopEvent : IEvent {
    internal object Start : LoopEvent(), IPosting by NamedProfilerEventBus("start")
    internal object Tick : LoopEvent(), IPosting by NamedProfilerEventBus("tick")
    internal object Render : LoopEvent(), IPosting by NamedProfilerEventBus("render")
    internal object RenderPost : LoopEvent(), IPosting by NamedProfilerEventBus("renderPost")
    internal object End : LoopEvent(), IPosting by NamedProfilerEventBus("end")
}