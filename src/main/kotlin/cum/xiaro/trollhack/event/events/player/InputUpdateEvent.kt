package cum.xiaro.trollhack.event.events.player

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventBus
import cum.xiaro.trollhack.event.EventPosting
import cum.xiaro.trollhack.event.WrappedForgeEvent
import net.minecraftforge.client.event.InputUpdateEvent

class InputUpdateEvent(override val event: InputUpdateEvent) : Event, WrappedForgeEvent, EventPosting by Companion {
    val movementInput
        get() = event.movementInput

    companion object : EventBus()
}