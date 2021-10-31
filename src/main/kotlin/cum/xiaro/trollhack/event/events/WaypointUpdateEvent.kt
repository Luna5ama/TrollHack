package cum.xiaro.trollhack.event.events

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventBus
import cum.xiaro.trollhack.event.EventPosting
import cum.xiaro.trollhack.manager.managers.WaypointManager.Waypoint

class WaypointUpdateEvent(val type: Type, val waypoint: Waypoint?) : Event, EventPosting by Companion {
    enum class Type {
        GET, ADD, REMOVE, CLEAR, RELOAD
    }

    companion object : EventBus()
}