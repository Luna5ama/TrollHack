package dev.luna5ama.trollhack.manager.managers

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import dev.luna5ama.trollhack.event.api.*
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.event.impl.world.ConnectionEvent
import dev.luna5ama.trollhack.event.impl.world.WorldEvent
import dev.luna5ama.trollhack.manager.AbstractManager
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB

object EntityManager : AbstractManager() , AlwaysListening {
    private var entity0 = emptyList<Entity>()
    private var livingBase0 = emptyList<LivingEntity>()
    private var players0 = emptyList<Player>()

    val entity: List<Entity>
        get() = entity0
    val livingBase: List<LivingEntity>
        get() = livingBase0
    val players: List<Player>
        get() = players0

    private var entityByID = Int2ObjectMaps.emptyMap<Entity>()
    private var livingBaseByID = Int2ObjectMaps.emptyMap<LivingEntity>()
    private var playersByID = Int2ObjectMaps.emptyMap<Player>()

    init {
        handler<ConnectionEvent.Disconnect>(Int.MAX_VALUE, true) {
            entity0 = emptyList()
            livingBase0 = emptyList()
            players0 = emptyList()

            entityByID = Int2ObjectMaps.emptyMap()
            livingBaseByID = Int2ObjectMaps.emptyMap()
            playersByID = Int2ObjectMaps.emptyMap()
        }

        handler<WorldEvent.Entity.Add>(Int.MAX_VALUE, true) {
            entity0 = entity0 + it.entity
            val entityByID = Int2ObjectOpenHashMap(this.entityByID)
            entityByID.remove(it.entity.id)
            this.entityByID = entityByID

            if (it.entity is LivingEntity) {
                livingBase0 = livingBase0 + it.entity
                val livingBaseByID = Int2ObjectOpenHashMap(this.livingBaseByID)
                livingBaseByID.remove(it.entity.id)
                this.livingBaseByID = livingBaseByID

                if (it.entity is Player) {
                    players0 = players0 + it.entity
                    val playersByID = Int2ObjectOpenHashMap(this.playersByID)
                    playersByID.remove(it.entity.id)
                    this.playersByID = playersByID
                }
            }
        }

        handler<WorldEvent.Entity.Remove>(Int.MAX_VALUE,true) {
            entity0 = entity0 - it.entity
            val entityByID = Int2ObjectOpenHashMap(this.entityByID)
            entityByID.remove(it.entity.id)
            this.entityByID = entityByID

            if (it.entity is LivingEntity) {
                livingBase0 = livingBase0 - it.entity
                val livingBaseByID = Int2ObjectOpenHashMap(this.livingBaseByID)
                livingBaseByID.remove(it.entity.id)
                this.livingBaseByID = livingBaseByID

                if (it.entity is Player) {
                    players0 = players0 - it.entity
                    val playersByID = Int2ObjectOpenHashMap(this.playersByID)
                    playersByID.remove(it.entity.id)
                    this.playersByID = playersByID
                }
            }
        }

        nonNullParallelHandler<TickEvent.Post> {
            entity0 = world.entitiesForRendering().toList()
            livingBase0 = world.entitiesForRendering().filterIsInstance<LivingEntity>()
            players0 = world.players.toList()

            entityByID = mapFromList(entity0)
            livingBaseByID = mapFromList(livingBase0)
            playersByID = mapFromList(players0)
        }
    }

    private fun <T : Entity> mapFromList(list: List<T>): Int2ObjectMap<T> {
        val map = Int2ObjectOpenHashMap<T>(list.size)
        for (i in list.indices) {
            val entity = list[i]
            map[entity.id] = entity
        }
        return map
    }

    fun getEntityByID(id: Int): Entity? = entityByID[id]

    fun getLivingBaseByID(id: Int): LivingEntity? = livingBaseByID[id]

    fun getPlayerByID(id: Int): Player? = playersByID[id]

    fun checkNoEntityCollision(box: AABB): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it.boundingBox.intersects(box) }
            .none()
    }

    fun checkNoEntityCollision(box: BlockPos): Boolean {
        return checkNoEntityCollision(AABB(box))
    }

    fun checkNoEntityCollision(box: AABB, ignoreEntity: Entity?): Boolean {
        if (ignoreEntity == null) return checkNoEntityCollision(box)

        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it != ignoreEntity }
            .filter { it.boundingBox.intersects(box) }
            .none()
    }

    fun checkEntityCollision(box: AABB, predicate: (Entity) -> Boolean): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
//            .filter { it.preventEntitySpawning }
            .filter { it.boundingBox.intersects(box) }
            .filter(predicate)
            .none()
    }

    fun checkEntityCollision(box: AABB, ignoreEntity: Entity?): Boolean {
        if (ignoreEntity == null) return checkEntityCollision(box)

        return entity.asSequence()
            .filter { it.isAlive }
//            .filter { it.preventEntitySpawning }
            .filter { it != ignoreEntity || it.startRiding(ignoreEntity) }
            .filter { it.boundingBox.intersects(box) }
            .none()


    }

    fun checkEntityCollision(box: AABB): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
//            .filter { it.preventEntitySpawning }
            .filter { it.boundingBox.intersects(box) }
            .none()
    }

    fun checkAnyEntity(box: AABB, predicate: (Entity) -> Boolean): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it.boundingBox.intersects(box) }
            .filter(predicate)
            .none()
    }

    fun checkAnyEntity(box: AABB, ignoreEntity: Entity): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it != ignoreEntity }
            .filter { it.boundingBox.intersects(box) }
            .none()
    }

    fun checkAnyEntity(box: AABB): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it.boundingBox.intersects(box) }
            .none()
    }
}
