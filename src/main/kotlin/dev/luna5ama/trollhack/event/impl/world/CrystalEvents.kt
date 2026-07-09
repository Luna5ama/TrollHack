package dev.luna5ama.trollhack.event.impl.world

import dev.luna5ama.trollhack.event.api.EventBus
import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.event.api.IPosting
import dev.luna5ama.trollhack.utils.world.explosion.advanced.CrystalDamage
import net.minecraft.world.entity.boss.enderdragon.EndCrystal

class CrystalSpawnEvent(
    val entityID: Int,
    val crystalDamage: CrystalDamage
) : IEvent, IPosting by Companion {
    companion object : EventBus()
}

class CrystalSetDeadEvent(
    val x: Double,
    val y: Double,
    val z: Double,
    val crystals: List<EndCrystal>
) : IEvent, IPosting by Companion {
    companion object : EventBus()
}