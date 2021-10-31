package cum.xiaro.trollhack.event.events.render

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventBus
import cum.xiaro.trollhack.event.EventPosting

class ResolutionUpdateEvent(val width: Int, val height: Int) : Event, EventPosting by Companion {
    companion object : EventBus()
}