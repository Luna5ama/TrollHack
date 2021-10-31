package cum.xiaro.trollhack.event.events

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventBus
import cum.xiaro.trollhack.event.EventPosting

sealed class ConnectionEvent : Event {
    object Connect : ConnectionEvent(), EventPosting by EventBus()
    object Disconnect : ConnectionEvent(), EventPosting by EventBus()
}