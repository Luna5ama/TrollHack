package cum.xiaro.trollhack.event.events

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventPosting
import cum.xiaro.trollhack.event.NamedProfilerEventBus

sealed class TickEvent : Event {
    object Pre : TickEvent(), EventPosting by NamedProfilerEventBus("trollTickPre")
    object Post : TickEvent(), EventPosting by NamedProfilerEventBus("trollTickPost")
}