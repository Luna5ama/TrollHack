package dev.luna5ama.trollhack.event.events.combat

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import dev.luna5ama.trollhack.util.combat.CrystalDamage

class CrystalSpawnEvent(
    val entityID: Int,
    val crystalDamage: CrystalDamage
) : Event, EventPosting by Companion {
    companion object : EventBus()
}