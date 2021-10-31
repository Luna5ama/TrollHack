package cum.xiaro.trollhack.event.events

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventBus
import cum.xiaro.trollhack.event.EventPosting
import net.minecraft.entity.EntityLivingBase

sealed class EntityEvent(val entity: EntityLivingBase) : Event {
    class UpdateHealth(entity: EntityLivingBase, val prevHealth: Float, val health: Float) : EntityEvent(entity), EventPosting by Companion {
        companion object : EventBus()
    }

    class Death(entity: EntityLivingBase) : EntityEvent(entity), EventPosting by Companion {
        companion object : EventBus()
    }
}