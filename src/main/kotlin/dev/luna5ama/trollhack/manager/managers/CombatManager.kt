package dev.luna5ama.trollhack.manager.managers

import com.google.common.collect.MapMaker
import dev.fastmc.common.sort.ObjectIntrosort
import it.unimi.dsi.fastutil.ints.Int2LongMaps
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.api.nonNullParallelHandler
import dev.luna5ama.trollhack.event.impl.LoopEvent
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.event.impl.world.*
import dev.luna5ama.trollhack.manager.AbstractManager
import dev.luna5ama.trollhack.modules.AbstractModule
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.collections.FastObjectArrayList
import dev.luna5ama.trollhack.utils.collections.compareFloatByDescending
import dev.luna5ama.trollhack.utils.combat.MotionTracker
import dev.luna5ama.trollhack.utils.extension.flooredPosition
import dev.luna5ama.trollhack.utils.math.floorToInt
import dev.luna5ama.trollhack.utils.math.vectors.VectorUtils.setAndAdd
import dev.luna5ama.trollhack.utils.math.vectors.distanceSqTo
import dev.luna5ama.trollhack.utils.math.vectors.toVec3
import dev.luna5ama.trollhack.utils.runIf
import dev.luna5ama.trollhack.utils.threads.Coroutine
import dev.luna5ama.trollhack.utils.threads.RenderThreadExecutor
import dev.luna5ama.trollhack.utils.threads.isActiveOrFalse
import dev.luna5ama.trollhack.utils.timing.TickTimer
import dev.luna5ama.trollhack.utils.world.CrystalUtils
import dev.luna5ama.trollhack.utils.world.explosion.advanced.CalcContext
import dev.luna5ama.trollhack.utils.world.explosion.advanced.CrystalDamage
import dev.luna5ama.trollhack.utils.world.explosion.advanced.DamageReduction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.sqrt

object CombatManager : AbstractManager(), AlwaysListening {
    private val combatModules: List<AbstractModule>

    private val damageReductionTimer = TickTimer()
    private val damageReductions = MapMaker().weakKeys().makeMap<LivingEntity, DamageReduction>()
    private val hurtTimeMap = Int2LongMaps.synchronize(Int2LongOpenHashMap()).apply { defaultReturnValue(-1L) }

    var target: LivingEntity? = null
        get() {
            if (field?.isAlive == false) {
                field = null
            }

            return field
        }
        set(value) {
            if (value != field) {
                CombatEvent.UpdateTarget(field, value).post()
                field = value
            }
        }
    var targetList = emptySet<LivingEntity>()

    var trackerSelf: MotionTracker? = null; private set
    var trackerTarget: MotionTracker? = null

    var contextSelf: CalcContext? = null; private set
    var contextTarget: CalcContext? = null; private set

    private val crystalTimer = TickTimer()
    private val removeTimer = TickTimer()

    private var placeJob: Job? = null
    private var crystalJob: Job? = null

    private val placeMap0 = ConcurrentHashMap<BlockPos, CrystalDamage>()
    private val crystalMap0 = MapMaker().weakKeys().makeMap<EndCrystal, CrystalDamage>()
    val placeMap: Map<BlockPos, CrystalDamage>
        get() = placeMap0
    val crystalMap: Map<EndCrystal, CrystalDamage>
        get() = crystalMap0

    var placeList = emptyList<CrystalDamage>(); private set
    var crystalList = emptyList<Pair<EndCrystal, CrystalDamage>>(); private set

    var modifyPos: BlockPos? = null
    var modifyBlockState: BlockState = Blocks.AIR.defaultBlockState()

    private const val PLACE_RANGE = 6
    private const val PLACE_RANGE_SQ = 36.0
    private const val CRYSTAL_RANGE_SQ = 144.0

    private const val MAX_RANGE_SQ = 256.0

    init {
        nonNullHandler<PacketEvent.Receive>(114514) { event ->
            when (event.packet) {
                is ClientboundSoundPacket -> {
                    if (event.packet.source != SoundSource.BLOCKS) return@nonNullHandler
                    if (event.packet.sound != SoundEvents.GENERIC_EXPLODE) return@nonNullHandler
                    val list = crystalList.asSequence()
                        .map(Pair<EndCrystal, CrystalDamage>::first)
                        .filter { it.distanceSqTo(event.packet.x, event.packet.y, event.packet.z) <= 144.0 }
                        .runIf(ClientSettings.crystalSetDead) { onEach { it.remove(Entity.RemovalReason.KILLED) } }
                        .toList()

                    if (list.isNotEmpty() && ClientSettings.crystalSetDead) {
                        RenderThreadExecutor.execute {
                            list.forEach {
                                world.removeEntity(it.id, Entity.RemovalReason.KILLED)
                            }
                        }
                    }
                    CrystalSetDeadEvent(event.packet.x, event.packet.y, event.packet.z, list).post()
                }
                is ClientboundAddEntityPacket -> {
                    if (event.packet.type == EntityType.ENDER_PEARL) {
                        val distSq = player.eyePosition.distanceSqTo(event.packet.x, event.packet.y, event.packet.z)
                        if (distSq > CRYSTAL_RANGE_SQ) return@nonNullHandler

                        val blockPos = BlockPos(
                            event.packet.x.floorToInt(),
                            event.packet.y.floorToInt() - 1,
                            event.packet.z.floorToInt()
                        )
                        getCrystalDamage(blockPos)?.let {
                            CrystalSpawnEvent(event.packet.id, it).post()
                        }
                    }
                }
                is ClientboundDamageEventPacket -> {
                    val entityID = event.packet.entityId
                    val time = System.currentTimeMillis()
                    hurtTimeMap[entityID] = time
                }
//                is EntityStatusS2CPacket -> {
//                    when (event.packet.status) {
//                        in 47..52 -> {
//                            hurtTimeMap[event.packet.getEntity(world)!!.id] = System.currentTimeMillis()
//                        }
//                    }
//                }
//                is SPacketEntityMetadata -> {
//                    val dataManagerEntries = event.packet.dataManagerEntries ?: return@safeListener
//                    val entity = world.getEntityByID(event.packet.entityId) as? EntityLivingBase? ?: return@safeListener
//                    val entry = dataManagerEntries.find {
//                        it.isDirty && it.key == runCatching { AccessorEntityLivingBase.trollGetHealthDataKey() }.getOrNull()
//                    } ?: return@safeListener
//
//                    (entry.value as? Float)?.let {
//                        var prevHealth = healthMap[entity.entityId]
//                        if (prevHealth.isNaN()) prevHealth = entity.health
//                        EntityEvent.UpdateHealth(entity, prevHealth, it).post()
//                        healthMap[entity.entityId] = it
//
//                        if (it <= 0.0f) {
//                            EntityEvent.Death(entity).post()
//                            if (event.packet.entityId == target?.entityId) {
//                                target = null
//                            }
//                            entity.deathTime
//                        }
//                    }
//                }
                is ClientboundRemoveEntitiesPacket -> {
                    event.packet.entityIds.forEach {
                        if (it == target?.id) target = null
                        hurtTimeMap.remove(it)
                    }
                }
            }
        }

        handler<ConnectionEvent.Disconnect> {
            damageReductions.clear()
            hurtTimeMap.clear()

            target = null
            targetList = emptySet()

            trackerSelf = null
            trackerTarget = null

            contextSelf = null
            contextTarget = null

            placeMap0.clear()
            crystalMap0.clear()

            placeList = emptyList()
            crystalList = emptyList()
        }

        nonNullHandler<WorldEvent.Entity.Add> { event ->
            when (event.entity) {
                is Player -> {
                    damageReductions[event.entity] = DamageReduction(event.entity)
                }

                is EndCrystal -> {
                    val distSq = event.entity.distanceSqTo(player.eyePosition)
                    if (distSq > CRYSTAL_RANGE_SQ) return@nonNullHandler

                    val contextSelf = contextSelf ?: return@nonNullHandler
                    val contextTarget = CombatManager.contextTarget

                    val blockPos = event.entity.blockPosition()
                    val mutableBlockPos = BlockPos.MutableBlockPos()
                    val crystalDamage = placeMap0.computeIfAbsent(blockPos) {
                        calculateDamage(
                            contextSelf,
                            contextTarget,
                            mutableBlockPos,
                            blockPos.toVec3(0.5, 1.0, 0.5),
                            blockPos,
                            sqrt(distSq)
                        )
                    }
                    crystalMap0[event.entity] = crystalDamage
                }
            }
        }

        handler<WorldEvent.Entity.Remove> {
            when (it.entity) {
                is LivingEntity -> {
                    damageReductions.remove(it.entity)
                }

                is EndCrystal -> {
                    crystalMap0.remove(it.entity)
                }
            }

            if (it.entity === target) target = null
            hurtTimeMap.remove(it.entity.id)
        }

        nonNullParallelHandler<TickEvent.Post> {
            val trackerSelf = trackerSelf?.takeIf { it.entity === player }
                ?: MotionTracker(player)

            trackerSelf.tick()
            CombatManager.trackerSelf = trackerSelf

            trackerTarget?.tick()
        }

        nonNullHandler<LoopEvent.Tick>(Int.MAX_VALUE) {
            val flag1 = damageReductionTimer.tickAndReset(ClientSettings.crystalUpdateDelay)
            val flag2 = crystalTimer.tickAndReset(ClientSettings.crystalUpdateDelay)

            if (flag1 || flag2) {
                if (flag1) {
                    EntityManager.players.forEach {
                        damageReductions[it] = DamageReduction(it)
                    }
                    target?.let {
                        damageReductions[it] = DamageReduction(it)
                    }
                }

                if (flag2) {
                    updateCrystalDamage()
                }
            }
        }
    }

    fun getCrystalDamage(crystal: EndCrystal) =
        crystalMap0[crystal] ?: getCrystalDamage(crystal.blockPosition())

    fun getCrystalDamage(blockPos: BlockPos) =
        contextSelf?.let { contextSelf ->
            placeMap0.computeIfAbsent(blockPos) {
                val crystalPos = blockPos.toVec3(0.5, 1.0, 0.5)
                val dist = contextSelf.entity.eyePosition.distanceTo(crystalPos)
                calculateDamage(contextSelf, contextTarget, BlockPos.MutableBlockPos(), crystalPos, it, dist)
            }
        }

    fun getDamageReduction(entity: LivingEntity) =
        damageReductions[entity]

    fun getHurtTime(entity: LivingEntity): Long {
        synchronized(hurtTimeMap) {
            var hurtTime = hurtTimeMap[entity.id]

            if (hurtTime == -1L) {
                val hurtTimeTicks = entity.hurtTime
                if (hurtTimeTicks != 0) {
                    hurtTime = System.currentTimeMillis() - hurtTimeTicks * 50L
                    hurtTimeMap[entity.id] = hurtTime
                }
            }

            return hurtTime
        }
    }

    context (NonNullContext)
    private fun updateCrystalDamage() {
        val flag1 = !placeJob.isActiveOrFalse
        val flag2 = !crystalJob.isActiveOrFalse

        if (flag1 || flag2) {
            val predictPosSelf = trackerSelf?.calcPosAhead(ClientSettings.selfPredictTicks) ?: player.position()
            val contextSelf = CalcContext(this@NonNullContext, player, predictPosSelf)

            val target = CombatManager.target
            val contextTarget = target?.let {
                val predictPos = trackerTarget?.calcPosAhead(ClientSettings.targetPredictTicks) ?: it.position()
                CalcContext(this@NonNullContext, it, predictPos)
            }

            val remove = removeTimer.tickAndReset(100)

            CombatManager.contextSelf = contextSelf
            CombatManager.contextTarget = contextTarget

            if (flag1) {
                placeJob = Coroutine.launch {
                    updatePlaceMap(contextSelf, contextTarget, remove)
                    updatePlaceList()
                }
            }
            if (flag2) {
                crystalJob = Coroutine.launch {
                    updateCrystalMap(contextSelf, contextTarget, remove)
                    updateCrystalList()
                }
            }
        }

        damageReductionTimer.reset(ClientSettings.crystalUpdateDelay / -4)
    }

    context (NonNullContext)
    private fun updatePlaceMap(contextSelf: CalcContext, contextTarget: CalcContext?, remove: Boolean) {
        val eyePos = player.eyePosition
        val flooredPos = player.flooredPosition
        val mutableBlockPos = BlockPos.MutableBlockPos()

        placeMap0.values.removeIf { crystalDamage ->
            remove && (crystalDamage.crystalPos.distanceSqTo(eyePos) > MAX_RANGE_SQ
                || !CrystalUtils.canPlaceCrystal(crystalDamage.blockPos, null)
                || contextTarget != null
                && (crystalDamage.crystalPos.distanceSqTo(contextTarget.predictPos) > MAX_RANGE_SQ
                || !contextTarget.checkColliding(crystalDamage.crystalPos)))
        }

        placeMap0.replaceAll { blockPos, crystalDamage ->
            calculateDamage(
                contextSelf,
                contextTarget,
                mutableBlockPos,
                blockPos.toVec3(0.5, 1.0, 0.5),
                blockPos,
                eyePos.distanceTo(crystalDamage.crystalPos)
            )
        }

        val blockPos = BlockPos.MutableBlockPos()

        for (x in -PLACE_RANGE..PLACE_RANGE) {
            for (y in -PLACE_RANGE..PLACE_RANGE) {
                for (z in -PLACE_RANGE..PLACE_RANGE) {
                    blockPos.setAndAdd(flooredPos, x, y, z)
                    if (blockPos.y !in 0..255) continue

                    val crystalX = blockPos.x + 0.5
                    val crystalY = blockPos.y + 1.0
                    val crystalZ = blockPos.z + 0.5

                    val distSq = eyePos.distanceSqTo(crystalX, crystalY, crystalZ)
                    if (distSq > PLACE_RANGE_SQ) continue
                    if (placeMap0.containsKey(blockPos)) continue
                    if (!CrystalUtils.canPlaceCrystal(blockPos, null)) continue

                    val crystalPos = Vec3(crystalX, crystalY, crystalZ)
                    if (contextTarget != null) {
                        if (contextTarget.predictPos.distanceSqTo(crystalPos) > CRYSTAL_RANGE_SQ) continue
                        if (!contextTarget.checkColliding(crystalPos)) continue
                    }

                    val immutablePos = blockPos.immutable()
                    placeMap0[immutablePos] = calculateDamage(
                        contextSelf,
                        contextTarget,
                        mutableBlockPos,
                        crystalPos,
                        immutablePos,
                        sqrt(distSq)
                    )
                }
            }
        }
    }

    context (NonNullContext)
    private fun updateCrystalMap(
        contextSelf: CalcContext,
        contextTarget: CalcContext?,
        remove: Boolean
    ) {
        val eyePos = player.eyePosition
        val mutableBlockPos = BlockPos.MutableBlockPos()

        if (remove) {
            crystalMap0.keys.removeIf {
                it.distanceSqTo(eyePos) > MAX_RANGE_SQ
            }
        }

        crystalMap0.replaceAll { _, crystalDamage ->
            placeMap0.computeIfAbsent(crystalDamage.blockPos) {
                calculateDamage(
                    contextSelf,
                    contextTarget,
                    mutableBlockPos,
                    it.toVec3(0.5, 1.0, 0.5),
                    it,
                    eyePos.distanceTo(crystalDamage.crystalPos)
                )
            }
        }

        for (entity in EntityManager.entity) {
            if (!entity.isAlive) continue
            if (entity !is EndCrystal) continue

            val distSq = entity.distanceSqTo(eyePos)
            if (distSq > CRYSTAL_RANGE_SQ) continue

            crystalMap0.computeIfAbsent(entity) {
                placeMap0.computeIfAbsent(entity.blockPosition()) {
                    calculateDamage(
                        contextSelf,
                        contextTarget,
                        mutableBlockPos,
                        it.toVec3(0.5, 1.0, 0.5),
                        it,
                        sqrt(distSq)
                    )
                }
            }
        }
    }

    private fun calculateDamage(
        contextSelf: CalcContext,
        contextTarget: CalcContext?,
        mutableBlockPos: BlockPos.MutableBlockPos,
        crystalPos: Vec3,
        blockPos: BlockPos,
        distance: Double
    ): CrystalDamage {
        val selfDamage = max(
            contextSelf.calcDamage(crystalPos, true, mutableBlockPos),
            contextSelf.calcDamage(crystalPos, false, mutableBlockPos)
        )
        val targetDamage = contextTarget?.calcDamage(crystalPos, true, mutableBlockPos) ?: 0.0f
        return CrystalDamage(
            crystalPos,
            blockPos,
            selfDamage,
            targetDamage,
            distance,
            contextSelf.currentPos.distanceTo(crystalPos)
        )
    }

    private fun updatePlaceList() {
        val list = FastObjectArrayList.wrap(placeMap.values.toTypedArray())
        ObjectIntrosort.sort(list.elements(), 0, list.size, compareFloatByDescending { it.targetDamage })
        placeList = list
    }

    private fun updateCrystalList() {
        val entries = crystalMap.entries
        val list = FastObjectArrayList.wrap(arrayOfNulls<Pair<EndCrystal, CrystalDamage>>(entries.size), 0)
        for ((crystal, crystalDamage) in entries) {
            list.add(crystal to crystalDamage)
        }
        ObjectIntrosort.sort(list.elements(), 0, list.size, compareFloatByDescending { it.second.targetDamage })
        crystalList = list
    }

    fun isActiveAndTopPriority(module: AbstractModule) = module.isActive && isOnTopPriority(module)

    fun isOnTopPriority(module: AbstractModule): Boolean {
        return getTopPriority() <= module.priority
    }

    fun getTopPriority(): Int {
        return getTopModule()?.priority ?: -1
    }

    fun getTopModule(): AbstractModule? {
        var topModule: AbstractModule? = null
        for (module in combatModules) {
            if (!module.isActive) continue
            if (module.priority < (topModule?.priority ?: 0)) continue
            topModule = module
        }
        return topModule
    }

    /** Use to mark a module that should be added to [combatModules] */
    annotation class CombatModule

    init {
        val cacheList = ArrayList<AbstractModule>()
        val annotationClass = CombatModule::class.java
        for (module in ModuleManager.modules) {
            if (module.category != Category.COMBAT) continue
            if (!module.javaClass.isAnnotationPresent(annotationClass)) continue
            cacheList.add(module)
        }
        combatModules = cacheList
    }
}