package dev.luna5ama.trollhack.event.events

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import dev.luna5ama.trollhack.manager.managers.WaypointManager.Waypoint

class WaypointUpdateEvent(val type: Type, val waypoint: Waypoint?) : Event, EventPosting by Companion {
    enum class Type {
        GET, ADD, REMOVE, CLEAR, RELOAD
    }

    companion object : EventBus()
}