package cum.xiaro.trollhack.event.events.player

import cum.xiaro.trollhack.event.Cancellable
import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventBus
import cum.xiaro.trollhack.event.EventPosting
import net.minecraft.entity.Entity

class PlayerAttackEvent(val entity: Entity) : Event, Cancellable(), EventPosting by Companion {
    companion object : EventBus()
}