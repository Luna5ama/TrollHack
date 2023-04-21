package dev.luna5ama.trollhack.event.events.player

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import dev.luna5ama.trollhack.event.WrappedForgeEvent
import net.minecraftforge.client.event.InputUpdateEvent

class InputUpdateEvent(override val event: InputUpdateEvent) : Event, WrappedForgeEvent, EventPosting by Companion {
    val movementInput
        get() = event.movementInput

    companion object : EventBus()
}