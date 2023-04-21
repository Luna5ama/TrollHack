package dev.luna5ama.trollhack.event.events.combat

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import net.minecraft.entity.EntityLivingBase

sealed class CombatEvent : Event {
    abstract val entity: EntityLivingBase?

    class UpdateTarget(val prevEntity: EntityLivingBase?, override val entity: EntityLivingBase?) : CombatEvent(),
        EventPosting by Companion {
        companion object : EventBus()
    }
}
