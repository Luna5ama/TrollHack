package cum.xiaro.trollhack.event.events.combat

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventBus
import cum.xiaro.trollhack.event.EventPosting
import cum.xiaro.trollhack.util.combat.CrystalDamage

class CrystalSpawnEvent(
    val entityID: Int,
    val crystalDamage: CrystalDamage
) : Event, EventPosting by Companion {
    companion object : EventBus()
}