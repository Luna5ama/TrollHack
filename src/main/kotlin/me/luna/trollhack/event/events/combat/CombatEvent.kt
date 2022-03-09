package me.luna.trollhack.event.events.combat

import me.luna.trollhack.event.Event
import me.luna.trollhack.event.EventBus
import me.luna.trollhack.event.EventPosting
import net.minecraft.entity.EntityLivingBase

sealed class CombatEvent : Event {
    abstract val entity: EntityLivingBase?

    class UpdateTarget(val prevEntity: EntityLivingBase?, override val entity: EntityLivingBase?) : CombatEvent(), EventPosting by Companion {
        companion object : EventBus()
    }
}
