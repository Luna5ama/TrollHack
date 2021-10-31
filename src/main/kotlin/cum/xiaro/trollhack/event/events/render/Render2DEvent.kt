package cum.xiaro.trollhack.event.events.render

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventPosting
import cum.xiaro.trollhack.event.NamedProfilerEventBus

sealed class Render2DEvent : Event {
    object Mc : Render2DEvent(), EventPosting by NamedProfilerEventBus("mc")
    object Absolute : Render2DEvent(), EventPosting by NamedProfilerEventBus("absolute")
    object Troll : Render2DEvent(), EventPosting by NamedProfilerEventBus("troll")
}