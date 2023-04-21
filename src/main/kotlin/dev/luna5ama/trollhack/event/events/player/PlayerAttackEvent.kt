package dev.luna5ama.trollhack.event.events.player

import dev.luna5ama.trollhack.event.Cancellable
import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import net.minecraft.entity.Entity

class PlayerAttackEvent(val entity: Entity) : Event, Cancellable(), EventPosting by Companion {
    companion object : EventBus()
}