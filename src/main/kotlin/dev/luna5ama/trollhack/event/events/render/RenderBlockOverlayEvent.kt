package dev.luna5ama.trollhack.event.events.render

import dev.luna5ama.trollhack.event.*
import net.minecraftforge.client.event.RenderBlockOverlayEvent

class RenderBlockOverlayEvent(override val event: RenderBlockOverlayEvent) : Event, ICancellable, WrappedForgeEvent,
    EventPosting by Companion {
    val type: RenderBlockOverlayEvent.OverlayType by event::overlayType

    companion object : EventBus()
}