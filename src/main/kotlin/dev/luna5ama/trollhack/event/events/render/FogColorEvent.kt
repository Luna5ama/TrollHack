package dev.luna5ama.trollhack.event.events.render

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import dev.luna5ama.trollhack.event.WrappedForgeEvent
import net.minecraftforge.client.event.EntityViewRenderEvent

class FogColorEvent(override val event: EntityViewRenderEvent.FogColors) : Event, WrappedForgeEvent,
    EventPosting by Companion {
    var red by event::red
    var green by event::green
    var blue by event::blue

    companion object : EventBus()
}