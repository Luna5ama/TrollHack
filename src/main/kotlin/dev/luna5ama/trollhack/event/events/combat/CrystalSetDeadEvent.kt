package dev.luna5ama.trollhack.event.events.combat

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import net.minecraft.entity.item.EntityEnderCrystal

class CrystalSetDeadEvent(
    val x: Double,
    val y: Double,
    val z: Double,
    val crystals: List<EntityEnderCrystal>
) : Event, EventPosting by Companion {
    companion object : EventBus()
}