package dev.luna5ama.trollhack.event.events

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import net.minecraft.entity.EntityLivingBase

sealed class EntityEvent(val entity: EntityLivingBase) : Event {
    class UpdateHealth(entity: EntityLivingBase, val prevHealth: Float, val health: Float) : EntityEvent(entity),
        EventPosting by Companion {
        companion object : EventBus()
    }

    class Death(entity: EntityLivingBase) : EntityEvent(entity), EventPosting by Companion {
        companion object : EventBus()
    }
}