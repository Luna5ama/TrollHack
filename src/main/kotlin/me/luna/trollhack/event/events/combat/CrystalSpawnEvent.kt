package me.luna.trollhack.event.events.combat

import me.luna.trollhack.event.Event
import me.luna.trollhack.event.EventBus
import me.luna.trollhack.event.EventPosting
import me.luna.trollhack.util.combat.CrystalDamage

class CrystalSpawnEvent(
    val entityID: Int,
    val crystalDamage: CrystalDamage
) : Event, EventPosting by Companion {
    companion object : EventBus()
}