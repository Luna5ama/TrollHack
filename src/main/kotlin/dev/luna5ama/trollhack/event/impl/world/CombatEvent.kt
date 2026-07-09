package dev.luna5ama.trollhack.event.impl.world

import dev.luna5ama.trollhack.event.api.EventBus
import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.event.api.IPosting
import net.minecraft.world.entity.LivingEntity

sealed class CombatEvent : IEvent {
    abstract val entity: LivingEntity?

    class UpdateTarget(val prevEntity: LivingEntity?, override val entity: LivingEntity?) : CombatEvent(),
        IPosting by Companion {
        companion object : EventBus()
    }
}
