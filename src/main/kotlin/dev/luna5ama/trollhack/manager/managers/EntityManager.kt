package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.util.math.intersectsBlock
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

object EntityManager : Manager() {
    private var entity0 = emptyList<Entity>()
    val entity: List<Entity>
        get() = entity0

    private var livingBase0 = emptyList<EntityLivingBase>()
    val livingBase: List<EntityLivingBase>
        get() = livingBase0

    private var players0 = emptyList<EntityPlayer>()
    val players: List<EntityPlayer>
        get() = players0

    private var entityByID = Int2ObjectMaps.emptyMap<Entity>()
    private var livingBaseByID = Int2ObjectMaps.emptyMap<EntityLivingBase>()
    private var playersByID = Int2ObjectMaps.emptyMap<EntityPlayer>()

    init {
        listener<ConnectionEvent.Disconnect>(Int.MAX_VALUE, true) {
            entity0 = emptyList()
            livingBase0 = emptyList()
            players0 = emptyList()

            entityByID = Int2ObjectMaps.emptyMap()
            livingBaseByID = Int2ObjectMaps.emptyMap()
            playersByID = Int2ObjectMaps.emptyMap()
        }

        listener<WorldEvent.Entity.Add>(Int.MAX_VALUE, true) {
            entity0 = entity0 + it.entity
            val entityByID = Int2ObjectOpenHashMap(this.entityByID)
            entityByID.remove(it.entity.entityId)
            this.entityByID = entityByID

            if (it.entity is EntityLivingBase) {
                livingBase0 = livingBase0 + it.entity
                val livingBaseByID = Int2ObjectOpenHashMap(this.livingBaseByID)
                livingBaseByID.remove(it.entity.entityId)
                this.livingBaseByID = livingBaseByID

                if (it.entity is EntityPlayer) {
                    players0 = players0 + it.entity
                    val playersByID = Int2ObjectOpenHashMap(this.playersByID)
                    playersByID.remove(it.entity.entityId)
                    this.playersByID = playersByID
                }
            }
        }

        listener<WorldEvent.Entity.Remove>(Int.MAX_VALUE, true) {
            entity0 = entity0 - it.entity
            val entityByID = Int2ObjectOpenHashMap(this.entityByID)
            entityByID.remove(it.entity.entityId)
            this.entityByID = entityByID

            if (it.entity is EntityLivingBase) {
                livingBase0 = livingBase0 - it.entity
                val livingBaseByID = Int2ObjectOpenHashMap(this.livingBaseByID)
                livingBaseByID.remove(it.entity.entityId)
                this.livingBaseByID = livingBaseByID

                if (it.entity is EntityPlayer) {
                    players0 = players0 - it.entity
                    val playersByID = Int2ObjectOpenHashMap(this.playersByID)
                    playersByID.remove(it.entity.entityId)
                    this.playersByID = playersByID
                }
            }
        }

        safeParallelListener<TickEvent.Post> {
            entity0 = world.loadedEntityList.toList()
            livingBase0 = world.loadedEntityList.filterIsInstance<EntityLivingBase>()
            players0 = world.playerEntities.toList()

            entityByID = mapFromList(entity0)
            livingBaseByID = mapFromList(livingBase0)
            playersByID = mapFromList(players0)
        }
    }

    private fun <T : Entity> mapFromList(list: List<T>): Int2ObjectMap<T> {
        val map = Int2ObjectOpenHashMap<T>(list.size)
        for (i in list.indices) {
            val entity = list[i]
            map[entity.entityId] = entity
        }
        return map
    }

    fun getEntityByID(id: Int): Entity? = entityByID[id]

    fun getLivingBaseByID(id: Int): EntityLivingBase? = livingBaseByID[id]

    fun getPlayerByID(id: Int): EntityPlayer? = playersByID[id]

    fun checkNoEntityCollision(box: AxisAlignedBB, predicate: (Entity) -> Boolean): Boolean {
        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.preventEntitySpawning }
            .filter { it.entityBoundingBox.intersects(box) }
            .filter(predicate)
            .none()
    }

    fun checkNoEntityCollision(box: AxisAlignedBB, ignoreEntity: Entity?): Boolean {
        if (ignoreEntity == null) return checkNoEntityCollision(box)

        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.preventEntitySpawning }
            .filter { it != ignoreEntity || it.isRidingSameEntity(ignoreEntity) }
            .filter { it.entityBoundingBox.intersects(box) }
            .none()
    }

    fun checkNoEntityCollision(pos: BlockPos, ignoreEntity: Entity?): Boolean {
        if (ignoreEntity == null) return checkNoEntityCollision(pos)

        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.preventEntitySpawning }
            .filter { it != ignoreEntity || it.isRidingSameEntity(ignoreEntity) }
            .filter { it.entityBoundingBox.intersectsBlock(pos) }
            .none()
    }

    fun checkNoEntityCollision(box: AxisAlignedBB): Boolean {
        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.preventEntitySpawning }
            .filter { it.entityBoundingBox.intersects(box) }
            .none()
    }

    fun checkNoEntityCollision(pos: BlockPos): Boolean {
        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.preventEntitySpawning }
            .filter { it.entityBoundingBox.intersectsBlock(pos) }
            .none()
    }

    fun checkNoEntity(box: AxisAlignedBB, predicate: (Entity) -> Boolean): Boolean {
        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.entityBoundingBox.intersects(box) }
            .filter(predicate)
            .none()
    }

    fun checkNoEntity(box: AxisAlignedBB, ignoreEntity: Entity): Boolean {
        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it != ignoreEntity }
            .filter { it.entityBoundingBox.intersects(box) }
            .none()
    }

    fun checkNoEntity(box: AxisAlignedBB): Boolean {
        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.entityBoundingBox.intersects(box) }
            .none()
    }

    fun checkNoEntity(pos: BlockPos): Boolean {
        return entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.entityBoundingBox.intersectsBlock(pos) }
            .none()
    }
}
