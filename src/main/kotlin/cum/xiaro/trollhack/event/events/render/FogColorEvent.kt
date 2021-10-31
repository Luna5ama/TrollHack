package cum.xiaro.trollhack.event.events.render

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventBus
import cum.xiaro.trollhack.event.EventPosting
import cum.xiaro.trollhack.event.WrappedForgeEvent
import net.minecraftforge.client.event.EntityViewRenderEvent

class FogColorEvent(override val event: EntityViewRenderEvent.FogColors) : Event, WrappedForgeEvent, EventPosting by Companion {
    var red by event::red
    var green by event::green
    var blue by event::blue

    companion object : EventBus()
}