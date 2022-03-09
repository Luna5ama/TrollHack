package me.luna.trollhack.event.events.render

import me.luna.trollhack.event.Event
import me.luna.trollhack.event.EventBus
import me.luna.trollhack.event.EventPosting
import me.luna.trollhack.event.WrappedForgeEvent
import net.minecraftforge.client.event.EntityViewRenderEvent

class FogColorEvent(override val event: EntityViewRenderEvent.FogColors) : Event, WrappedForgeEvent, EventPosting by Companion {
    var red by event::red
    var green by event::green
    var blue by event::blue

    companion object : EventBus()
}