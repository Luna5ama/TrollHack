package cum.xiaro.trollhack.event.events.combat

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventBus
import cum.xiaro.trollhack.event.EventPosting
import net.minecraft.entity.item.EntityEnderCrystal

class CrystalSetDeadEvent(
    val x: Double,
    val y: Double,
    val z: Double,
    val crystals: List<EntityEnderCrystal>
) : Event, EventPosting by Companion {
    companion object : EventBus()
}