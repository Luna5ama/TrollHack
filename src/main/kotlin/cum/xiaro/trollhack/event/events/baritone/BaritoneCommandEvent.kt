package cum.xiaro.trollhack.event.events.baritone

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventBus
import cum.xiaro.trollhack.event.EventPosting

class BaritoneCommandEvent(val command: String) : Event, EventPosting by Companion {
    companion object : EventBus()
}