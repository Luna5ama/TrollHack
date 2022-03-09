package me.luna.trollhack.event.events.player

import me.luna.trollhack.event.Cancellable
import me.luna.trollhack.event.Event
import me.luna.trollhack.event.EventBus
import me.luna.trollhack.event.EventPosting
import net.minecraft.entity.Entity

class PlayerAttackEvent(val entity: Entity) : Event, Cancellable(), EventPosting by Companion {
    companion object : EventBus()
}