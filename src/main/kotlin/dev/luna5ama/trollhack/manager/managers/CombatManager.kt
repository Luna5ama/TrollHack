package dev.luna5ama.trollhack.manager.managers

import com.google.common.collect.MapMaker
import dev.fastmc.common.TickTimer
import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.common.floorToInt
import dev.fastmc.common.sort.ObjectIntrosort
import dev.luna5ama.trollhack.event.*
import dev.luna5ama.trollhack.event.events.*
import dev.luna5ama.trollhack.event.events.combat.CombatEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSetDeadEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSpawnEvent
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.mixins.accessor.entity.AccessorEntityLivingBase
import dev.luna5ama.trollhack.module.AbstractModule
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.ModuleManager
import dev.luna5ama.trollhack.module.modules.combat.CombatSetting
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.EntityUtils.flooredPosition
import dev.luna5ama.trollhack.util.EntityUtils.isFakeOrSelf
import dev.luna5ama.trollhack.util.accessor.entityID
import dev.luna5ama.trollhack.util.collections.compareFloatByDescending
import dev.luna5ama.trollhack.util.combat.CalcContext
import dev.luna5ama.trollhack.util.combat.CrystalDamage
import dev.luna5ama.trollhack.util.combat.CrystalUtils.blockPos
import dev.luna5ama.trollhack.util.combat.CrystalUtils.canPlaceCrystal
import dev.luna5ama.trollhack.util.combat.DamageReduction
import dev.luna5ama.trollhack.util.combat.MotionTracker
import dev.luna5ama.trollhack.util.extension.synchronized
import dev.luna5ama.trollhack.util.math.VectorUtils.setAndAdd
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.math.vector.toVec3d
import dev.luna5ama.trollhack.util.runIf
import dev.luna5ama.trollhack.util.threads.*
import it.unimi.dsi.fastutil.ints.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.server.*
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.sqrt

object CombatManager : Manager() {
    private val combatModules: List<AbstractModule>

    private val playerAttackTimeMap = WeakHashMap<EntityPlayer, Long>().synchronized()
    private val damageReductionTimer = TickTimer()
    private val damageReductions = MapMaker().weakKeys().makeMap<EntityLivingBase, DamageReduction>()
    private val hurtTimeMap = Int2LongMaps.synchronize(Int2LongOpenHashMap()).apply { defaultReturnValue(-1L) }
    private val healthMap = Int2FloatMaps.synchronize(Int2FloatOpenHashMap()).apply { defaultReturnValue(Float.NaN) }

    var target: EntityLivingBase? = null
        get() {
            if (field?.isEntityAlive == false) {
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
    var targetList = emptySet<EntityLivingBase>()

    var trackerSelf: MotionTracker? = null; private set
    var trackerTarget: MotionTracker? = null

    var contextSelf: CalcContext? = null; private set
    var contextTarget: CalcContext? = null; private set

    private val crystalTimer = TickTimer()
    private val removeTimer = TickTimer()

    private var placeJob: Job? = null
    private var crystalJob: Job? = null

    private val placeMap0 = ConcurrentHashMap<BlockPos, CrystalDamage>()
    private val crystalMap0 = MapMaker().weakKeys().makeMap<EntityEnderCrystal, CrystalDamage>()
    val placeMap: Map<BlockPos, CrystalDamage>
        get() = placeMap0
    val crystalMap: Map<EntityEnderCrystal, CrystalDamage>
        get() = crystalMap0

    var placeList = emptyList<CrystalDamage>(); private set
    var crystalList = emptyList<Pair<EntityEnderCrystal, CrystalDamage>>(); private set

    private const val PLACE_RANGE = 6
    private const val PLACE_RANGE_SQ = 36.0
    private const val CRYSTAL_RANGE_SQ = 144.0

    private const val MAX_RANGE_SQ = 256.0

    init {
        safeListener<PacketEvent.Receive>(114514) { event ->
            when (event.packet) {
                is SPacketSoundEffect -> {
                    if (event.packet.category != SoundCategory.BLOCKS) return@safeListener
                    if (event.packet.sound != SoundEvents.ENTITY_GENERIC_EXPLODE) return@safeListener
                    val list = crystalList.asSequence()
                        .map(Pair<EntityEnderCrystal, CrystalDamage>::first)
                        .filter { it.distanceSqTo(event.packet.x, event.packet.y, event.packet.z) <= 144.0 }
                        .runIf(CombatSetting.crystalSetDead) { onEach(EntityEnderCrystal::setDead) }
                        .toList()

                    if (list.isNotEmpty() && CombatSetting.crystalSetDead) {
                        onMainThreadSafe {
                            list.forEach {
                                world.removeEntity(it)
                                world.removeEntityDangerously(it)
                            }
                        }
                    }
                    CrystalSetDeadEvent(event.packet.x, event.packet.y, event.packet.z, list).post()
                }

                is SPacketSpawnObject -> {
                    if (event.packet.type == 51) {
                        val distSq = player.eyePosition.distanceSqTo(event.packet.x, event.packet.y, event.packet.z)
                        if (distSq > CRYSTAL_RANGE_SQ) return@safeListener

                        val blockPos = BlockPos(
                            event.packet.x.floorToInt(),
                            event.packet.y.floorToInt() - 1,
                            event.packet.z.floorToInt()
                        )
                        getCrystalDamage(blockPos)?.let {
                            CrystalSpawnEvent(event.packet.entityID, it).post()
                        }
                    }
                }

                is SPacketAnimation -> {
                    if (event.packet.animationType == 1) {
                        val entityID = event.packet.entityID
                        val time = System.currentTimeMillis()
                        hurtTimeMap[entityID] = time
                    }
                }

                is SPacketCombatEvent -> {
                    if (event.packet.eventType == SPacketCombatEvent.Event.ENTITY_DIED && event.packet.playerId == player.entityId) {
                        EntityEvent.Death(player).post()
                    }
                }

                is SPacketEntityStatus -> {
                    when (event.packet.opCode.toInt()) {
                        3 -> {
                            (event.packet.getEntity(world) as? EntityLivingBase)?.let {
                                EntityEvent.Death(it).post()
                            }
                            if (event.packet.entityID == target?.entityId) {
                                target = null
                            }
                        }

                        2, 33, 36, 37 -> {
                            hurtTimeMap[event.packet.entityID] = System.currentTimeMillis()
                        }
                    }
                }

                is SPacketEntityMetadata -> {
                    val dataManagerEntries = event.packet.dataManagerEntries ?: return@safeListener
                    val entity = world.getEntityByID(event.packet.entityId) as? EntityLivingBase? ?: return@safeListener
                    val entry = dataManagerEntries.find {
                        it.isDirty && it.key == runCatching { AccessorEntityLivingBase.trollGetHealthDataKey() }.getOrNull()
                    } ?: return@safeListener

                    (entry.value as? Float)?.let {
                        var prevHealth = healthMap[entity.entityId]
                        if (prevHealth.isNaN()) prevHealth = entity.health
                        EntityEvent.UpdateHealth(entity, prevHealth, it).post()
                        healthMap[entity.entityId] = it

                        if (it <= 0.0f) {
                            EntityEvent.Death(entity).post()
                            if (event.packet.entityId == target?.entityId) {
                                target = null
                            }
                            entity.deathTime
                        }
                    }
                }

                is SPacketDestroyEntities -> {
                    event.packet.entityIDs.forEach {
                        if (it == target?.entityId) target = null
                        hurtTimeMap.remove(it)
                        healthMap.remove(it)
                    }
                }
            }
        }

        listener<ConnectionEvent.Disconnect> {
            damageReductions.clear()
            hurtTimeMap.clear()
            healthMap.clear()

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

        safeListener<WorldEvent.Entity.Add> { event ->
            when (event.entity) {
                is EntityPlayer -> {
                    damageReductions[event.entity] = DamageReduction(event.entity)
                }

                is EntityEnderCrystal -> {
                    val distSq = event.entity.distanceSqTo(player.eyePosition)
                    if (distSq > CRYSTAL_RANGE_SQ) return@safeListener

                    val contextSelf = contextSelf ?: return@safeListener
                    val contextTarget = CombatManager.contextTarget

                    DefaultScope.launch {
                        val blockPos = event.entity.blockPos
                        val mutableBlockPos = BlockPos.MutableBlockPos()
                        val crystalDamage = placeMap0.computeIfAbsent(blockPos) {
                            calculateDamage(
                                contextSelf,
                                contextTarget,
                                mutableBlockPos,
                                blockPos.toVec3d(0.5, 1.0, 0.5),
                                blockPos,
                                sqrt(distSq)
                            )
                        }

                        crystalMap0[event.entity] = crystalDamage
                    }
                }
            }
        }

        listener<WorldEvent.Entity.Remove> {
            when (it.entity) {
                is EntityLivingBase -> {
                    damageReductions.remove(it.entity)
                }

                is EntityEnderCrystal -> {
                    crystalMap0.remove(it.entity)
                }
            }

            if (it.entity === target) target = null
            hurtTimeMap.remove(it.entity.entityId)
            healthMap.remove(it.entity.entityId)
        }

        safeParallelListener<TickEvent.Post> {
            val trackerSelf = trackerSelf?.takeIf { it.entity === player }
                ?: MotionTracker(player)

            trackerSelf.tick()
            CombatManager.trackerSelf = trackerSelf

            trackerTarget?.tick()

            val attacked = player.lastAttackedEntity
            if (attacked is EntityPlayer && attacked.isEntityAlive && !attacked.isFakeOrSelf) {
                playerAttackTimeMap[attacked] =
                    System.currentTimeMillis() - (player.ticksExisted - player.lastAttackedEntityTime) * 50L
            }
        }

        safeListener<RunGameLoopEvent.Tick>(Int.MAX_VALUE) {
            val flag1 = damageReductionTimer.tickAndReset(CombatSetting.crystalUpdateDelay)
            val flag2 = crystalTimer.tickAndReset(CombatSetting.crystalUpdateDelay)

            if (flag1 || flag2) {
                playerController.updateController()

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

    fun getPlayerAttackTime(player: EntityPlayer): Long {
        return playerAttackTimeMap[player] ?: -1L
    }

    fun getCrystalDamage(crystal: EntityEnderCrystal) =
        crystalMap0[crystal] ?: getCrystalDamage(crystal.blockPos)

    fun getCrystalDamage(blockPos: BlockPos) =
        contextSelf?.let { contextSelf ->
            placeMap0.computeIfAbsent(blockPos) {
                val crystalPos = blockPos.toVec3d(0.5, 1.0, 0.5)
                val dist = contextSelf.entity.eyePosition.distanceTo(crystalPos)
                calculateDamage(contextSelf, contextTarget, BlockPos.MutableBlockPos(), crystalPos, it, dist)
            }
        }

    fun getDamageReduction(entity: EntityLivingBase) =
        damageReductions[entity]

    fun getHurtTime(entity: EntityLivingBase): Long {
        synchronized(hurtTimeMap) {
            var hurtTime = hurtTimeMap[entity.entityId]

            if (hurtTime == -1L) {
                val hurtTimeTicks = entity.hurtTime
                if (hurtTimeTicks != 0) {
                    hurtTime = System.currentTimeMillis() - hurtTimeTicks * 50L
                    hurtTimeMap[entity.entityId] = hurtTime
                }
            }

            return hurtTime
        }
    }

    private fun SafeClientEvent.updateCrystalDamage() {
        val flag1 = !placeJob.isActiveOrFalse
        val flag2 = !crystalJob.isActiveOrFalse

        if (flag1 || flag2) {
            val predictPosSelf = trackerSelf?.calcPosAhead(CombatSetting.getPredictTicksSelf()) ?: player.positionVector
            val contextSelf = CalcContext(this, player, predictPosSelf)

            val target = CombatManager.target
            val contextTarget = target?.let {
                val predictPos = trackerTarget?.calcPosAhead(CombatSetting.getPredictTicksTarget()) ?: it.positionVector
                CalcContext(this, it, predictPos)
            }

            val remove = removeTimer.tickAndReset(100)

            CombatManager.contextSelf = contextSelf
            CombatManager.contextTarget = contextTarget

            if (flag1) {
                placeJob = BackgroundScope.launch {
                    updatePlaceMap(contextSelf, contextTarget, remove)
                    updatePlaceList()
                }
            }
            if (flag2) {
                crystalJob = BackgroundScope.launch {
                    updateCrystalMap(contextSelf, contextTarget, remove)
                    updateCrystalList()
                }
            }
        }

        damageReductionTimer.reset(CombatSetting.crystalUpdateDelay / -4)
    }

    private fun SafeClientEvent.updatePlaceMap(contextSelf: CalcContext, contextTarget: CalcContext?, remove: Boolean) {
        val eyePos = player.eyePosition
        val flooredPos = player.flooredPosition
        val mutableBlockPos = BlockPos.MutableBlockPos()

        placeMap0.values.removeIf { crystalDamage ->
            remove && (crystalDamage.crystalPos.distanceSqTo(eyePos) > MAX_RANGE_SQ
                || !canPlaceCrystal(crystalDamage.blockPos, null)
                || contextTarget != null
                && (crystalDamage.crystalPos.distanceSqTo(contextTarget.predictPos) > MAX_RANGE_SQ
                || !contextTarget.checkColliding(crystalDamage.crystalPos)))
        }

        placeMap0.replaceAll { blockPos, crystalDamage ->
            calculateDamage(
                contextSelf,
                contextTarget,
                mutableBlockPos,
                blockPos.toVec3d(0.5, 1.0, 0.5),
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
                    if (!canPlaceCrystal(blockPos, null)) continue

                    val crystalPos = Vec3d(crystalX, crystalY, crystalZ)
                    if (contextTarget != null) {
                        if (contextTarget.predictPos.distanceSqTo(crystalPos) > CRYSTAL_RANGE_SQ) continue
                        if (!contextTarget.checkColliding(crystalPos)) continue
                    }

                    val immutablePos = blockPos.toImmutable()
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

    private fun SafeClientEvent.updateCrystalMap(
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
                    it.toVec3d(0.5, 1.0, 0.5),
                    it,
                    eyePos.distanceTo(crystalDamage.crystalPos)
                )
            }
        }

        for (entity in EntityManager.entity) {
            if (!entity.isEntityAlive || !entity.isAddedToWorld) continue
            if (entity !is EntityEnderCrystal) continue

            val distSq = entity.distanceSqTo(eyePos)
            if (distSq > CRYSTAL_RANGE_SQ) continue

            crystalMap0.computeIfAbsent(entity) {
                placeMap0.computeIfAbsent(entity.blockPos) {
                    calculateDamage(
                        contextSelf,
                        contextTarget,
                        mutableBlockPos,
                        it.toVec3d(0.5, 1.0, 0.5),
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
        crystalPos: Vec3d,
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
        val list = FastObjectArrayList.wrap(arrayOfNulls<Pair<EntityEnderCrystal, CrystalDamage>>(entries.size), 0)
        for ((crystal, crystalDamage) in entries) {
            list.add(crystal to crystalDamage)
        }
        ObjectIntrosort.sort(list.elements(), 0, list.size, compareFloatByDescending { it.second.targetDamage })
        crystalList = list
    }

    fun isActiveAndTopPriority(module: AbstractModule) = module.isActive() && isOnTopPriority(module)

    fun isOnTopPriority(module: AbstractModule): Boolean {
        return getTopPriority() <= module.modulePriority
    }

    fun getTopPriority(): Int {
        return getTopModule()?.modulePriority ?: -1
    }

    fun getTopModule(): AbstractModule? {
        var topModule: AbstractModule? = null
        for (module in combatModules) {
            if (!module.isActive()) continue
            if (module.modulePriority < (topModule?.modulePriority ?: 0)) continue
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