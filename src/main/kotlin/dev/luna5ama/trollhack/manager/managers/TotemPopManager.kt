package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.EntityEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.combat.TotemPopEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.util.accessor.entityID
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.SPacketEntityStatus

object TotemPopManager : Manager() {
    private val trackerMap = Int2ObjectMaps.synchronize(Int2ObjectOpenHashMap<Tracker>())

    init {
        listener<ConnectionEvent.Disconnect>(true) {
            trackerMap.clear()
        }

        listener<EntityEvent.Death>(5000, true) { event ->
            if (event.entity !is EntityPlayer) return@listener
            if (event.entity == mc.player) {
                trackerMap.clear()
            } else {
                trackerMap.remove(event.entity.entityId)?.let {
                    TotemPopEvent.Death(event.entity, it.count).post()
                }
            }
        }

        safeListener<PacketEvent.Receive>(true) {
            if (it.packet is SPacketEntityStatus && it.packet.opCode.toInt() == 35) {
                val entity = it.packet.getEntity(world) as? EntityPlayer? ?: return@safeListener
                val tracker = trackerMap.computeIfAbsent(it.packet.entityID) {
                    Tracker(entity.entityId, entity.name)
                }
                tracker.updateCount()
                TotemPopEvent.Pop(entity, tracker.count).post()
            }
        }

        safeParallelListener<TickEvent.Post>(true) {
            for (entity in EntityManager.players) {
                trackerMap[entity.entityId]?.update()
            }

            val removeTime = System.currentTimeMillis()
            val iterator = trackerMap.values.iterator()

            while (iterator.hasNext()) {
                val tracker = iterator.next()
                if (tracker.timeout < removeTime) {
                    TotemPopEvent.Clear(tracker.name, tracker.count).post()
                    iterator.remove()
                }
            }
        }
    }

    fun getPopCount(entity: Entity): Int {
        return trackerMap[entity.entityId]?.count ?: 0
    }

    fun getTracker(entity: Entity): Tracker? {
        return trackerMap[entity.entityId]
    }

    class Tracker(val entityID: Int, val name: String) {
        var count = 0; private set
        var timeout = System.currentTimeMillis() + 15000L; private set
        var popTime = 0L; private set

        fun update() {
            timeout = System.currentTimeMillis() + 15000L
        }

        fun updateCount() {
            synchronized(this) {
                count++
                timeout = System.currentTimeMillis() + 15000L
                popTime = System.currentTimeMillis()
            }
        }
    }
}