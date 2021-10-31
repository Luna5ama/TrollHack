package cum.xiaro.trollhack.event.events

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventPosting
import cum.xiaro.trollhack.event.NamedProfilerEventBus

sealed class ProcessKeyBindEvent : Event {
    object Pre : ProcessKeyBindEvent(), EventPosting by NamedProfilerEventBus("pre")
    object Post : ProcessKeyBindEvent(), EventPosting by NamedProfilerEventBus("post")
}