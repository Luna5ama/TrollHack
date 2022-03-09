package me.luna.trollhack.event.events.player

import me.luna.trollhack.event.Event
import me.luna.trollhack.event.EventBus
import me.luna.trollhack.event.EventPosting
import me.luna.trollhack.event.WrappedForgeEvent
import net.minecraftforge.client.event.InputUpdateEvent

class InputUpdateEvent(override val event: InputUpdateEvent) : Event, WrappedForgeEvent, EventPosting by Companion {
    val movementInput
        get() = event.movementInput

    companion object : EventBus()
}