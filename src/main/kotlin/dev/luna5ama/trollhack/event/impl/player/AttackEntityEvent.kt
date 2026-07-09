package dev.luna5ama.trollhack.event.impl.player

import dev.luna5ama.trollhack.event.api.*
import net.minecraft.world.entity.Entity

class AttackEntityEvent(val entity: Entity) : IEvent, ICancellable by Cancellable(), IPosting by AttackEntityEvent {
    companion object : EventBus()
}