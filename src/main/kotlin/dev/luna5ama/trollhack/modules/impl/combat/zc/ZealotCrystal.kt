package dev.luna5ama.trollhack.modules.impl.combat.zc

import dev.fastmc.common.BlockPosUtil.toLong
import it.unimi.dsi.fastutil.ints.Int2LongMaps
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.longs.Long2LongMaps
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.LoopEvent
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.event.impl.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.impl.render.Render2DEvent
import dev.luna5ama.trollhack.event.impl.render.Render3DEvent
import dev.luna5ama.trollhack.event.impl.world.WorldEvent
import dev.luna5ama.trollhack.manager.managers.*
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.serverSideItem
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.modules.impl.client.Colors
import dev.luna5ama.trollhack.modules.impl.player.PacketMine
import dev.luna5ama.trollhack.utils.*
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.utils.collections.CircularArray
import dev.luna5ama.trollhack.utils.collections.averageOrZero
import dev.luna5ama.trollhack.utils.collections.forEachFast
import dev.luna5ama.trollhack.utils.combat.BlockInteractionHelper
import dev.luna5ama.trollhack.utils.compat.armorStacksCompat
import dev.luna5ama.trollhack.utils.compat.isSwordCompat
import dev.luna5ama.trollhack.utils.compat.isToolCompat
import dev.luna5ama.trollhack.utils.delegates.DynamicCachedValue
import dev.luna5ama.trollhack.utils.delegates.CachedValueN
import dev.luna5ama.trollhack.utils.extension.*
import dev.luna5ama.trollhack.graphics.ESPRenderer
import dev.luna5ama.trollhack.graphics.animations.Easing
import dev.luna5ama.trollhack.graphics.buffer.Render3DUtils
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.utils.inventory.HotbarSlot
import dev.luna5ama.trollhack.utils.inventory.everySlots
import dev.luna5ama.trollhack.utils.inventory.hotbarSlots
import dev.luna5ama.trollhack.utils.inventory.pause.HandPause
import dev.luna5ama.trollhack.utils.inventory.pause.MainHandPause
import dev.luna5ama.trollhack.utils.inventory.pause.withPause
import dev.luna5ama.trollhack.utils.inventory.swapToSlot
import dev.luna5ama.trollhack.utils.math.*
import dev.luna5ama.trollhack.utils.math.vectors.*
import dev.luna5ama.trollhack.utils.math.vectors.VectorUtils.toViewVec
import dev.luna5ama.trollhack.utils.threads.Coroutine
import dev.luna5ama.trollhack.utils.threads.RenderThreadExecutor
import dev.luna5ama.trollhack.utils.threads.isActiveOrFalse
import dev.luna5ama.trollhack.utils.timing.TickTimer
import dev.luna5ama.trollhack.utils.world.*
import dev.luna5ama.trollhack.utils.world.explosion.advanced.CrystalDamage
import dev.luna5ama.trollhack.utils.world.explosion.advanced.ExposureSample
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.prediction.PredictiveAction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundSwingPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.phys.Vec3
import net.minecraft.world.Difficulty
import net.minecraft.world.InteractionHand
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureNanoTime

object ZealotCrystal : Module("Zealot Crystal", category = Category.COMBAT) {
    private val page by setting("Page", Page.General)

    private val players by setting("Players", true, { page == Page.General })
    private val animals by setting("Animals", true, { page == Page.General })
    private val mobs by setting("mobs", true, { page == Page.General })
    private val maxTargets by setting("Max Targets", 5, 1..10, 1, { page == Page.General })
    private val targetRange by setting("Target Range", 16.0f, 0.0f..32.0f, 1.0f, { page == Page.General })
    private val yawSpeed by setting("Yaw Speed", 45.0f, 5.0f..180.0f, 5.0f, { page == Page.General })
    private val eatingPause by setting("Eating Pause", false, { page == Page.General })
    private val updateDelay by setting("Update Delay Ms", 5, 0..250, 1, { page == Page.General })
    private val globalDelay by setting("Global Delay Nano", 1_000_000, 1_000..10_000_000, 1_000, { page == Page.General })

    private val placeMode by setting("Place Mode", PlaceMode.Single, { page == Page.Place })
    private val placeSwing by setting("Place Swing", false, { page == Page.Place })
    private val spamPlace by setting("Spam Place", false, { page == Page.Place })
    private val placeSwitchMode by setting("Place Switch Mode", SwitchMode.Off, { page == Page.Place })
    private val packetPlace by setting("Packet Place", PacketPlaceMode.Strong, { page == Page.Place })
    private val placeRange by setting("Place Range", 6.0f, 3.0f..8.0f, 0.25f, { page == Page.Place })
    private val placeRangeMode by setting("Place Range Mode", RangeMode.Feet, { page == Page.Place })
    private val placeMinDmg by setting("Place Min Damage", 5.0f, 0.0f..20.0f, 0.25f, { page == Page.Place })
    private val placeMaxSelf by setting("Place Max Self", 6.0f, 0.0f..20.0f, 0.25f, { page == Page.Place })
    private val placeBalance by setting("Place Balance", -3.0f, -10.0f..10.0f, 0.25f, { page == Page.Place })
    private val placeDelay by setting("Place Delay", 50, 0..1000, 25, { page == Page.Place })
    private val placeRotationRange by setting("Place Rotation Range", 120.0f, 0.0f..180.0f, 0.5f, { page == Page.Place })

    private val breakMode by setting("Break Mode", BreakMode.Smart, { page == Page.Break })
    private val antiWeakness by setting("Anti Weakness", SwitchMode.Off, { page == Page.Break })
    private val swapDelay by setting("Swap Delay", 0, 0..20, 1, { page == Page.Break })
    private val packetBreak by setting("Packet Break", BreakMode.Smart, { page == Page.Break })
    private val bbtt by setting("2B2T", false, { page == Page.Break })
    private val bbttFactor by setting("2B2T Factor", 200, 0..1000, 25, { page == Page.Break && bbtt })
    private val breakRange by setting("Break Range", 5.0f, 0.0f..8.0f, 0.1f, { page == Page.Break })
    private val breakRangeMode by setting("Break Range Mode", RangeMode.Feet, { page == Page.Break })
    private val breakMinDamage by setting("Break Min Damage", 4.0f, 0.0f..20.0f, 0.25f, { page == Page.Break })
    private val breakMaxSelf by setting("Break Max Self Damage", 8.0f, 0.0f..20.0f, 0.25f, { page == Page.Break })
    private val breakBalance by setting("Break Balance", -4.0f, -10.0f..10.0f, 0.25f, { page == Page.Break })
    private val breakDelay by setting("Break Delay", 50, 0..1000, 25, { page == Page.Break })
    private val crystalRotation by setting("Crystal Rotation", true, { page == Page.Break })
    private val breakRotationRange by setting("Break Rotation Range", 120.0f, 0.0f..180.0f, 0.5f, { page == Page.Break })

    private val forcePlaceHealth by setting("Force Place Health", 8.0f, 0.0f..20.0f, 0.5f, { page == Page.ForcePlace })
    private val forcePlaceArmorRate by setting("Force Place Armor Rate", 3, 0..25, 1, { page == Page.ForcePlace })
    private val forcePlaceMinDamage by setting("Force Place Min Damage", 1.5f, 0.0f..10.0f, 0.25f, { page == Page.ForcePlace })
    private val forcePlaceMotion by setting("Force Place Motion", 4.0f, 0.0f..10.0f, 0.25f, { page == Page.ForcePlace })
    private val forcePlaceBalance by setting("Force Place Balance", -1.0f, -10.0f..10.0f, 0.25f, { page == Page.ForcePlace })
    private val forcePlaceWhileSwording by setting("Force Place While Swording", false, { page == Page.ForcePlace })

    private val assumeInstantMine by setting("Assume Instant Mine", true, { page == Page.Calculation })
    private val noSuicide by setting("No Suicide", 2.0f, 0.0f..20.0f, 0.25f, { page == Page.Calculation })
    private val motionPredict by setting("Motion Predict", true, { page == Page.Calculation })
    private val predictTicks by setting("Motion Predict Ticks", 2, 1..10, 1, { page == Page.Calculation && motionPredict })
    private val placeBypass by setting("Place Bypass", PlaceBypass.Closest, { page == Page.Calculation})
    private val updateMaxMoveDist by setting("Update Movement", 2, 1..5, 1, { page == Page.Calculation })
    private val minMoveSpeed by setting("Min Move Speed", 0.1, { page == Page.Calculation})
    private val wallRange by setting("Wall Range", 3.0f, 0.0f..8.0f, 0.1f, { page == Page.Calculation })
    private val damagePriority by setting("Damage Priority", DamagePriority.Efficient, { page == Page.Calculation })
    private val lethalOverride by setting("Lethal Override", true, { page == Page.Calculation })
    private val lethalThresholdAddition by setting(
        "Lethal Threshole Addition",
        0.5f, -5.0f..5.0f, 0.1f, { page == Page.Calculation && lethalOverride })
    private val lethalMaxSelfDamage by setting(
        "Lethal Max Self Damage",
        16.0f, 0.0f..20.0f, 0.25f, { page == Page.Calculation && lethalOverride })
    private val safeMaxTargetDamageReduction by setting(
        "Safe Max Target Damage Reduction",
        1.0f, 0.0f..10.0f, 0.1f, { page == Page.Calculation })
    private val safeMinSelfDamageReduction by setting(
        "Safe Min Self Damage Reduction",
        2.0f, 0.0f..10.0f, 0.1f, { page == Page.Calculation })
    private val collidingCrystalExtraSelfDamageThreshold by setting(
        "Colliiding Crystal Extra Self Damage Threshold",
        4.0f, 0.0f..10.0f, 0.1f, { page == Page.Calculation })

    private val swingMode by setting("Swing Mode", SwingMode.Client, { page == Page.Misc })
    private val swingHand by setting("Swing Hand", SwingHand.Auto, { page == Page.Misc })

    private val filledAlpha by setting("Filled Alpha", 63, 0..255, 1, { page == Page.Visual })
    private val outlineAlpha by setting("Outline Alpha", 200, 0..255, 1, { page == Page.Visual })
    private val targetDamage by setting("Target Damage", true, { page == Page.Visual })
    private val selfDamage by setting("Self Damage", true, { page == Page.Visual })
    private val hudInfo by setting("Hud Info", HudInfo.SPEED, { page == Page.Visual })
    private val movingLength by setting("Moving Length", 400, 0..1000, 50, { page == Page.Visual })
    private val fadeLength by setting("Fade Length", 200, 0..1000, 50, { page == Page.Visual })

    @JvmStatic
    val target: LivingEntity?
        get() = placeInfo.getLazy()?.target

    private val crystalSpawnMap = Int2LongMaps.synchronize(Int2LongOpenHashMap())
    private val attackedCrystalMap = Int2LongMaps.synchronize(Int2LongOpenHashMap())
    private val attackedPosMap = Long2LongMaps.synchronize(Long2LongOpenHashMap())

    private val timeoutTimer = TickTimer()
    private val placeTimer = TickTimer()
    val breakTimer = TickTimer()

    var lastActiveTime = 0L; private set
    private var lastRotation: PlaceInfo? = null

    private val explosionTimer = TickTimer()
    private val explosionCountArray = CircularArray<Int>(8)
    private var explosionCount = 0

    private val calculationTimes = CircularArray<Int>(100)
    private var calculationTimesPending = IntArrayList()

    private val loopThread = Thread ({
        var updateTask: Job? = null
        var loopTask: Job? = null

        while (true) {
            val loopStart = System.nanoTime()
            try {
                while (isDisabled || mc.level == null) {
                    try {
                        Thread.sleep(1000L)
                    } catch (e: InterruptedException) {
                        break
                    }
                }

                if (!updateTask.isActiveOrFalse) {
                    updateTask = Coroutine.launch {
                        try {
                            targets.get(25L)
                            if (crystalRotation) rotationInfo.get(mc.deltaTracker.gameTimeDeltaTicks.toInt())
                            placeInfo.get(updateDelay)

                            if (explosionTimer.tickAndReset(250L)) {
                                val count = explosionCount
                                explosionCount = 0
                                explosionCountArray.add(count)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                if (!loopTask.isActiveOrFalse) {
                    loopTask = Coroutine.launch {
                        try {
                            runLoop()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                try {
                    val interval = globalDelay - (System.nanoTime() - loopStart)
                    if (interval > 0) Thread.sleep(interval / 1_000_000, (interval % 1_000_000).toInt())
                } catch (_: InterruptedException) {

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }, "$name-Loop").apply {
        isDaemon = true
        start()
    }

    private val blocks = DynamicCachedValue {
        runSafe { getPlaceableBlocks(crystalRotation, BlockPos.MutableBlockPos()) } ?: listOf()
    }
    private val blocksUpdateTime get() = runSafe {
        ((updateMaxMoveDist / max(player.offset.length(), minMoveSpeed))
                * mc.deltaTracker.gameTimeDeltaTicks).toLong()
    } ?: 0L

    private val renderTargetSet = CachedValueN(5L) {
        IntOpenHashSet().apply {
            targets.getLazy()?.forEach {
                add(it.entity.id)
            }
        }
    }

    private val targets = DynamicCachedValue {
        runSafe {
            getTargets()
        } ?: emptySequence()
    }

    private val rawPosList = DynamicCachedValue {
        runSafe {
            getRawPosList()
        } ?: emptyList()
    }

    private val rotationInfo = DynamicCachedValue(PlaceInfo.INVALID) {
        runSafe {
            calcPlaceInfo(false)
        }
    }

    private val placeInfo = DynamicCachedValue(PlaceInfo.INVALID) {
        runSafe {
            calcPlaceInfo(crystalRotation)
        }
    }

    private val renderPlaceInfo: PlaceInfo?
        get() = if (crystalRotation) rotationInfo.getLazy() else placeInfo.getLazy()

    override fun getDisplayInfo(): String {
        return when (hudInfo) {
            HudInfo.OFF -> ""
            HudInfo.SPEED -> "%.1f".format(explosionCountArray.averageOrZero() * 4.0)
            HudInfo.DAMAGE -> renderPlaceInfo?.let { "%.1f/%.1f".format(it.targetDamage, it.selfDamage) }
                ?: "0.0/0.0"
            HudInfo.TARGET -> target?.name?.string ?: "None"
            HudInfo.CALCULATION_TIME -> "%.2f ms".format(calculationTimes.averageOrZero() / 1_000_000.0)
        }
    }

    init {
        onEnabled {
            loopThread.interrupt()
        }

        onDisabled {
            placeTimer.reset(-114514L)
            breakTimer.reset(-114514L)

            lastActiveTime = 0L
            lastRotation = null

            explosionTimer.reset(-114514L)
            explosionCountArray.clear()
            explosionCount = 0

            calculationTimes.clear()

            Renderer.reset()
        }

        nonNullHandler<OnUpdateWalkingPlayerEvent.Pre>(114514) {
            if (paused()) return@nonNullHandler

            if (!crystalRotation) return@nonNullHandler

            var placing = System.currentTimeMillis() - lastActiveTime <= 250L
            rotationInfo.get(mc.deltaTracker.gameTimeDeltaTicks.toInt() * 2)?.let {
                lastRotation = it
                placing = true
            }

            if (placing) {
                lastRotation?.let {
                    val rotation = RotationUtils.getRotationTo(it.hitVec)
                    val diff = RotationUtils.calcAngleDiff(rotation.x, PlayerPacketManager.rotation.x)

                    if (abs(diff) <= yawSpeed) {
                        sendPlayerPacket {
                            rotate(rotation)
                        }
                    } else {
                        val clamped = diff.coerceIn(-yawSpeed, yawSpeed)
                        val newYaw = RotationUtils.normalizeAngle(PlayerPacketManager.rotation.x + clamped)

                        sendPlayerPacket {
                            rotate(Vec2f(newYaw, rotation.y))
                        }
                    }
                }
            } else {
                lastRotation = null
            }
        }

        nonNullHandler<TickEvent.Post> {
            for (entity in EntityManager.entity) {
                if (entity !is LivingEntity) continue
                reductionMap[entity] = DamageReduction(entity)
            }

            rawPosList.updateForce()
        }

        nonNullHandler<WorldEvent.ClientBlockUpdate>(114514) {
            if (player.distanceSqTo(it.pos.center) < (placeRange.ceilToInt() + 1).sq
                && checkResistant(it.pos, it.oldState) != checkResistant(it.pos, it.newState)
            ) {
                rawPosList.updateLazy()
                rotationInfo.updateLazy()
                placeInfo.updateLazy()
            }
        }

        nonNullHandler<LoopEvent.Render>(114514) {
            val list: IntArrayList
            synchronized(calculationTimes) {
                list = calculationTimesPending
                calculationTimesPending = IntArrayList()
            }
            calculationTimes.addAll(list)
        }

        nonNullHandler<PacketEvent.Receive>(114514) { event ->
            val packet = event.packet
            if (packet is ClientboundAddEntityPacket) {
                handleSpawnPacket(packet)
            } else if (packet is ClientboundSoundPacket) {
                handleExplosion(packet)
            }
        }

        nonNullHandler<Render3DEvent> {
            Renderer.onRender3D()
        }

        nonNullHandler<Render2DEvent> {
            with(it.context) {
                Renderer.onRender2D()
            }
        }
    }

    fun handleSpawnPacket(packet: ClientboundAddEntityPacket) {
        if (isDisabled || packet.type() != EntityType.END_CRYSTAL) return

        runSafe {
            val mutableBlockPos = BlockPos.MutableBlockPos()
            if (checkBreakRange(packet.x, packet.y, packet.z, mutableBlockPos)) {
                if (!paused() && !bbtt && checkCrystalRotation(packet.x, packet.y, packet.z)) {
                    placeInfo.getLazy()?.let {
                        when (packetBreak) {
                            BreakMode.Smart -> {
                                if (CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(
                                        it.blockPos,
                                        packet.x,
                                        packet.y,
                                        packet.z
                                    )
                                    || checkBreakDamage(packet.x, packet.y, packet.z, mutableBlockPos)
                                ) {
                                    breakDirect(packet.x, packet.y, packet.z, packet.id)
                                }
                            }
                            BreakMode.All -> {
                                breakDirect(packet.x, packet.y, packet.z, packet.id)
                            }
                            else -> {
                                return
                            }
                        }
                    }
                }

                crystalSpawnMap[packet.id] = System.currentTimeMillis()
            }
        }
    }

    private fun NonNullContext.checkBreakDamage(
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        val context = CombatManager.contextSelf ?: return false
        val selfDamage = max(
            context.calcDamage(crystalX, crystalY, crystalZ, false, mutableBlockPos),
            context.calcDamage(crystalX, crystalY, crystalZ, true, mutableBlockPos)
        )
        if (player.scaledHealth - selfDamage <= noSuicide) return false

        return targets.get(100L).any {
            checkBreakDamage(crystalX, crystalY, crystalZ, selfDamage, it, mutableBlockPos)
        }
    }

    private fun NonNullContext.checkBreakDamage(
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        selfDamage: Float,
        targetInfo: TargetInfo,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        val targetDamage =
            calcDamage(targetInfo.entity, targetInfo.pos, targetInfo.box, crystalX, crystalY, crystalZ, mutableBlockPos)
        if (lethalOverride && targetDamage - targetInfo.entity.totalHealth > lethalThresholdAddition && targetDamage <= lethalMaxSelfDamage) {
            return true
        }

        if (selfDamage > breakMaxSelf) return false

        val minDamage: Float
        val balance: Float

        if (shouldForcePlace(targetInfo.entity)) {
            minDamage = forcePlaceMinDamage
            balance = forcePlaceBalance
        } else {
            minDamage = breakMinDamage
            balance = breakBalance
        }

        return targetDamage >= minDamage && targetDamage - selfDamage >= balance
    }

    fun handleExplosion(packet: ClientboundSoundPacket) {
        if (isDisabled || packet.sound != SoundEvents.GENERIC_EXPLODE) return

        runSafe {
            val placeInfo = placeInfo.getLazy()
            if (placeInfo != null) {
                if (distanceSq(
                        placeInfo.blockPos.x + 0.5, placeInfo.blockPos.y + 1.0, placeInfo.blockPos.z + 0.5,
                        packet.x, packet.y, packet.z
                    ) <= 144.0
                ) {
                    if (packetPlace.onRemove) {
                        placeDirect(placeInfo)
                    }

                    if (attackedPosMap.containsKey(
                            toLong(
                                packet.x.floorToInt(),
                                packet.y.floorToInt(),
                                packet.z.floorToInt()
                            )
                        )
                    ) {
                        explosionCount++
                    }

                    crystalSpawnMap.clear()
                    attackedCrystalMap.clear()
                    attackedPosMap.clear()
                }
            } else if (player.distanceSqTo(packet.x, packet.y, packet.z) <= 144.0) {
                crystalSpawnMap.clear()
                attackedCrystalMap.clear()
                attackedPosMap.clear()
            }
        }
    }


    private fun paused(): Boolean {
        return false // TODO: Update pause logic
    }

    private fun runLoop() {
        if (paused()) return

        val breakFlag = breakMode != BreakMode.Off && breakTimer.tick(breakDelay)
        val placeFlag = placeMode != PlaceMode.Off && placeTimer.tick(placeDelay)

        if (timeoutTimer.tickAndReset(5L)) {
            updateTimeouts()
        }

        if (breakFlag || placeFlag) {
            runSafe {
                val placeInfo = placeInfo.get(updateDelay)
                placeInfo?.let {
                    if (checkPausing()) return
                    if (breakFlag) doBreak(placeInfo)
                    if (placeFlag) doPlace(placeInfo)
                }
            }
        }
    }

    private fun updateTimeouts() {
        val current = System.currentTimeMillis()

        crystalSpawnMap.runSynchronized {
            values.removeIf {
                it + 5000L < current
            }
        }

        attackedCrystalMap.runSynchronized {
            values.removeIf {
                it < current
            }
        }

        attackedPosMap.runSynchronized {
            values.removeIf {
                it < current
            }
        }
    }

    context (NonNullContext)
    private fun checkPausing(): Boolean {
        return eatingPause && player.usingItemHand != null && player.useItem.item.isFood
    }

    private fun NonNullContext.doPlace(placeInfo: PlaceInfo) {
        if (spamPlace || checkPlaceCollision(placeInfo)) {
            placeDirect(placeInfo)
        }
    }

    private fun checkPlaceCollision(placeInfo: PlaceInfo): Boolean {
        return EntityManager.entity.asSequence()
            .filterIsInstance<EndCrystal>()
            .filter { it.isAlive }
            .filter { CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(placeInfo.blockPos, it) }
            .filterNot { attackedCrystalMap.containsKey(it.id) }
            .none()
    }

    context (NonNullContext)
    private fun doBreak(placeInfo: PlaceInfo) {
        val crystalList = getCrystalList()

        val crystal = when (breakMode) {
            BreakMode.Smart -> {
                getTargetCrystal(placeInfo, crystalList)
                    ?: getCrystal(crystalList)
            }
            BreakMode.All -> {
                val entity = target ?: player
                crystalList.minByOrNull { entity.distanceTo(it) }
            }
            else -> {
                return
            }
        }

        crystal?.let {
            breakDirect(it.x, it.y, it.z, it.id)
        }
    }

    private fun NonNullContext.getHandNullable(): InteractionHand? {
        return when (net.minecraft.world.item.Items.END_CRYSTAL) {
            player.offhandItem.item -> InteractionHand.OFF_HAND
            player.offhandItem.item -> InteractionHand.MAIN_HAND
            else -> null
        }
    }

    context (NonNullContext)
    private fun placeDirect(placeInfo: PlaceInfo) {
        if (player.everySlots.countItem(Items.END_CRYSTAL) == 0) return

        val hand = getHandNullable()

        if (hand == null) {
            when (placeSwitchMode) {
                SwitchMode.Off -> {
                    return
                }
                SwitchMode.Legit -> {
                    val slot = player.getCrystalSlot(placeSwitchMode) ?: return
                    slot as HotbarSlot
                    MainHandPause.withPause(ZealotCrystal, placeDelay * 2) {
                        swapToSlot(slot)
                        sendSequencedPacket {
                            placePacket(placeInfo, InteractionHand.MAIN_HAND, it)
                        }
                    }
                }
                SwitchMode.Ghost, SwitchMode.Inventory -> {
                    val slot = player.getCrystalSlot(placeSwitchMode) ?: return
                    HotbarSwitchManager.ghostSwitch(slot) {
                        sendSequencedPacket {
                            placePacket(placeInfo, InteractionHand.MAIN_HAND, it)
                        }
                    }
                }
            }
        } else {
            HandPause[hand].withPause(ZealotCrystal, placeDelay * 2) {
                sendSequencedPacket {
                    placePacket(placeInfo, hand, it)
                }
            }
        }
        placeTimer.reset()
//        lastActiveTime = System.currentTimeMillis()

        if (placeSwing) {
            RenderThreadExecutor.execute {
                swingHand()
            }
        }
    }

    context (NonNullContext)
    private fun sendSequencedPacket(packetCreator: PredictiveAction) {
        world.blockStatePredictionHandler.startPredicting().use { pendingUpdateManager ->
            val i = pendingUpdateManager.currentSequenceNr
            val packet = packetCreator.predict(i)
            netHandler.send(packet)
        }
    }

    private fun placePacket(placeInfo: PlaceInfo, hand: InteractionHand, sequence: Int): ServerboundUseItemOnPacket {
        return ServerboundUseItemOnPacket(
            hand, BlockHitResult(placeInfo.hitVec, placeInfo.side, placeInfo.blockPos, false), sequence
        )
    }

    context (NonNullContext)
    private fun breakDirect(x: Double, y: Double, z: Double, entityID: Int) {
        if (System.currentTimeMillis() - HotbarSwitchManager.swapTime < swapDelay * 50L) return

        if (player.isWeaknessActive() && !isHoldingTool()) {
            when (antiWeakness) {
                SwitchMode.Off -> {
                    return
                }
                SwitchMode.Legit -> {
                    val slot = getWeaponSlot(antiWeakness) ?: return
                    slot as HotbarSlot
                    MainHandPause.withPause(ZealotCrystal, placeDelay * 2) {
                        swapToSlot(slot)
                        if (swapDelay != 0) return@withPause
                        netHandler.send(attackPacket(entityID))
                        swingHand()
                    }
                }
                SwitchMode.Ghost, SwitchMode.Inventory -> {
                    val slot = getWeaponSlot(antiWeakness) ?: return
                    HotbarSwitchManager.ghostSwitch(slot) {
                        if (swapDelay != 0) return@ghostSwitch
                        netHandler.send(attackPacket(entityID))
                        swingHand()
                    }
                }
            }
        } else {
            netHandler.send(attackPacket(entityID))
            swingHand()
        }
//        netHandler.sendPacket(attackPacket(entityID))
//        swingHand()

        attackedCrystalMap[entityID] = System.currentTimeMillis() + 1000L
        attackedPosMap[toLong(x.floorToInt(), y.floorToInt(), z.floorToInt())] = System.currentTimeMillis() + 1000L
        breakTimer.reset()

        lastActiveTime = System.currentTimeMillis()

        placeInfo.get(500L)?.let {
//            player.setLastAttackedEntity(it.target)
            if (packetPlace.onBreak && CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(it.blockPos, x, y, z)) {
                placeDirect(it)
            }
        }
    }

    private fun NonNullContext.swingHand() {
        val hand = when (swingHand) {
            SwingHand.Auto -> if (player.offhandItem.item.let { it == Items.END_CRYSTAL || it != Items.GOLDEN_APPLE })
                InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
            SwingHand.OffHand -> InteractionHand.OFF_HAND
            SwingHand.MainHand -> InteractionHand.MAIN_HAND
        }

        swingMode.swingHand(this, hand)
    }

    private fun attackPacket(entityID: Int): ServerboundInteractPacket {
        val packet = ServerboundInteractPacket(entityID, false, ServerboundInteractPacket.ATTACK_ACTION)
        return packet
    }

    private fun Player.isWeaknessActive(): Boolean {
        return this.hasEffect(MobEffects.WEAKNESS)
                && this.getEffect(MobEffects.STRENGTH)?.let {
            it.amplifier <= 0
        } != false
    }

    private fun NonNullContext.isHoldingTool(): Boolean {
        return player.serverSideItem.isToolCompat
    }

    private fun Player.getMaxCrystalSlot(): HotbarSlot? {
        return this.hotbarSlots.asSequence().filter {
            it.item.item == Items.END_CRYSTAL
        }.maxByOrNull {
            it.item.count
        }
    }

    private fun Player.getCrystalSlot(swapMode: SwitchMode): Slot? {
        if (swapMode == SwitchMode.Ghost || swapMode == SwitchMode.Legit) {
            return hotbarSlots.firstItem(Items.END_CRYSTAL)
        } else if (swapMode == SwitchMode.Inventory) {
            return everySlots.firstItem(Items.END_CRYSTAL)
        }
        return null
    }

    private fun NonNullContext.getWeaponSlot(swapMode: SwitchMode): Slot? {
        if (swapMode == SwitchMode.Ghost || swapMode == SwitchMode.Legit) {
            return player.hotbarSlots.filterByStack {
                it.isSwordCompat || it.isToolCompat
            }.firstOrNull()
        } else if (swapMode == SwitchMode.Inventory) {
            return player.everySlots.filterByStack {
                it.isSwordCompat || it.isToolCompat
            }.firstOrNull()
        }
        return null

    }

    context (NonNullContext)
    private fun getCrystalList(): List<EndCrystal> {
        val eyePos = PlayerPacketManager.position.add(0.0, player.eyeY - player.y, 0.0)
        val sight = eyePos.add(PlayerPacketManager.rotation.toViewVec().scale(8.0))
        val mutableBlockPos = BlockPos.MutableBlockPos()

        return EntityManager.entity.asSequence()
            .filterIsInstance<EndCrystal>()
            .filter { it.isAlive }
            .runIf(bbtt) {
                val current = System.currentTimeMillis()
                filter { current - getSpawnTime(it) >= bbttFactor }
            }
            .filter { checkBreakRange(it, mutableBlockPos) }
            .filter { checkCrystalRotation(it.boundingBox, eyePos, sight) }
            .toList()
    }

    private inline fun <T> T.runIf(boolean: Boolean, block: T.() -> T): T {
        return if (boolean) block.invoke(this)
        else this
    }

    private fun getSpawnTime(crystal: EndCrystal): Long {
        return crystalSpawnMap.computeIfAbsent(crystal.id) { Long.MIN_VALUE }
    }

    private fun getTargetCrystal(placeInfo: PlaceInfo, crystalList: List<EndCrystal>): EndCrystal? {
        return crystalList.firstOrNull {
            CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(placeInfo.blockPos, it.x, it.y, it.z)
        }
    }

    context (NonNullContext)
    @Suppress("DuplicatedCode")
    private fun getCrystal(crystalList: List<EndCrystal>): EndCrystal? {
        val max = BreakInfo.Mutable()
        val safe = BreakInfo.Mutable()
        val lethal = BreakInfo.Mutable()

        val targets = targets.get(25L).toList()

        val noSuicide = noSuicide
        val mutableBlockPos = BlockPos.MutableBlockPos()
        val context = CombatManager.contextSelf ?: return null
        val damagePriority = damagePriority

        if (targets.isNotEmpty()) {
            for (crystal in crystalList) {
                val selfDamage = max(
                    context.calcDamage(crystal.x, crystal.y, crystal.z, false, mutableBlockPos),
                    context.calcDamage(crystal.x, crystal.y, crystal.z, true, mutableBlockPos)
                )
                if (player.scaledHealth - selfDamage <= noSuicide) continue
                if (!lethalOverride && selfDamage > breakMaxSelf) continue

                for ((entity, entityPos, entityBox) in targets) {
                    val targetDamage = calcDamage(
                        entity,
                        entityPos,
                        entityBox,
                        crystal.x,
                        crystal.y,
                        crystal.z,
                        mutableBlockPos
                    )
                    if (lethalOverride && System.currentTimeMillis() - CombatManager.getHurtTime(entity) > 400L
                        && targetDamage - entity.totalHealth > lethalThresholdAddition && selfDamage < lethal.selfDamage
                        && selfDamage <= lethalMaxSelfDamage
                    ) {
                        lethal.update(crystal, selfDamage, targetDamage)
                    }

                    if (selfDamage > breakMaxSelf) continue

                    val minDamage: Float
                    val balance: Float

                    if (shouldForcePlace(entity)) {
                        minDamage = forcePlaceMinDamage
                        balance = forcePlaceBalance
                    } else {
                        minDamage = breakMinDamage
                        balance = breakBalance
                    }

                    if (targetDamage < minDamage || targetDamage - selfDamage < balance) continue

                    if (damagePriority(selfDamage, targetDamage) > damagePriority(max.selfDamage, max.targetDamage)) {
                        max.update(crystal, selfDamage, targetDamage)
                    } else if (max.targetDamage - targetDamage <= safeMaxTargetDamageReduction
                        && max.selfDamage - selfDamage >= safeMinSelfDamageReduction
                    ) {
                        safe.update(crystal, selfDamage, targetDamage)
                    }
                }
            }
        }

        if (max.targetDamage - safe.targetDamage > safeMaxTargetDamageReduction
            || max.selfDamage - safe.selfDamage <= safeMinSelfDamageReduction
        ) {
            safe.clear()
        }

        val valid = lethal.takeValid()
            ?: safe.takeValid()
            ?: max.takeValid()

        return valid?.crystal
    }

    context (NonNullContext)
    private fun getRawPosList(): List<BlockPos> {
        val mutableBlockPos = BlockPos.MutableBlockPos()

        val range = placeRange

        val rangeSq = range.sq
        val wallRangeSq = wallRange.sq

        val floor = range.floorToInt()
        val ceil = range.ceilToInt()

        val list = ArrayList<BlockPos>()
        val pos = BlockPos.MutableBlockPos()

        val feetPos = PlayerPacketManager.position

        val feetXInt = feetPos.x.floorToInt()
        val feetYInt = feetPos.y.floorToInt()
        val feetZInt = feetPos.z.floorToInt()

        val eyePos = PlayerPacketManager.eyePosition

        for (x in feetXInt - floor..feetXInt + ceil) {
            for (z in feetZInt - floor..feetZInt + ceil) {
                for (y in feetYInt - floor..feetYInt + ceil) {
                    pos.set(x, y, z)
                    if (!world.isInWorldBounds(pos) || !world.worldBorder.isWithinBounds(pos)) continue

                    val crystalX = pos.x + 0.5
                    val crystalY = pos.y + 1.0
                    val crystalZ = pos.z + 0.5

                    if (player.placeDistanceSq(crystalX, crystalY, crystalZ) > rangeSq) continue
                    if (!isPlaceable(pos)) continue
                    if (feetPos.distanceSqTo(crystalX, crystalY, crystalZ) > wallRangeSq
                        && !world.rayTraceVisible(eyePos, crystalX, crystalY + 1.7, crystalZ, 20, mutableBlockPos)
                    ) continue

                    list.add(pos.immutable())
                }
            }
        }

        list.sortByDescending { it.distanceSqTo(feetXInt, feetYInt, feetZInt) }

        return list
    }

    context (NonNullContext)
    private fun getPlaceableBlocks(checkRotation: Boolean, mutableBlockPos: BlockPos.MutableBlockPos): List<BlockPos> {
        val rangeSq = placeRange.sq
        val single = placeMode == PlaceMode.Single
        val feetPos = PlayerPacketManager.position
        val feetXInt = feetPos.x.floorToInt()
        val feetYInt = feetPos.y.floorToInt()
        val feetZInt = feetPos.z.floorToInt()
        val eyePos = PlayerPacketManager.eyePosition
        val sight = eyePos.add(PlayerPacketManager.rotation.toViewVec().scale(8.0))
        val collidingEntities = getCollidingEntities(rangeSq, feetXInt, feetYInt, feetZInt, single, mutableBlockPos)
        return buildList {
            BlockInteractionHelper.getSphere(player.blockPosition(), placeRange, placeRange.toInt(),
                false, true, 0)
                .filter { it.block == Blocks.OBSIDIAN || it.block == Blocks.BEDROCK }
                .filter { CrystalUtils.hasValidSpaceForCrystal(it, true) }
                // check rotation
                .filter { !checkRotation || checkPlaceRotation(it, eyePos, sight) }
                .filter { checkPlaceCollision(it, collidingEntities) }
                .forEach { add(it) }
        }
    }

    context (NonNullContext)
    private fun isPlaceable(
        pos: BlockPos
    ): Boolean {
        if (!CrystalUtils.canPlaceCrystalOn(pos)) {
            return false
        }
        return CrystalUtils.hasValidSpaceForCrystal(pos, true)
    }

    context (NonNullContext)
    private fun getTargets(): Sequence<TargetInfo> {
        val rangeSq = targetRange.sq
        val ticks = if (motionPredict) predictTicks else 0
        val list = ArrayList<TargetInfo>()
        val eyePos = PlayerPacketManager.eyePosition

        if (players) {
            for (target in EntityManager.players) {
                if (target == player) continue
                if (target == mc.cameraEntity) continue
                if (!target.isAlive) continue
                if (world.isOutsideBuildHeight(target.y.floorToInt())) continue
                if (target.distanceSqTo(eyePos) > rangeSq) continue
                if (FriendManager.isFriend(target.name.string)) continue

                list.add(getTargetInfo(target, ticks))
            }
        }

        if (mobs || animals) {
            for (target in EntityManager.entity) {
                if (target == player) continue
                if (!target.isAlive) continue
                if (world.isOutsideBuildHeight(target.y.floorToInt())) continue
                if (target !is LivingEntity) continue
                if (target is Player) continue
                if (target.distanceSqTo(eyePos) > rangeSq) continue
                if (!animals && target is Animal) continue

                val pos = target.position()
                list.add(
                    TargetInfo(
                        target,
                        pos,
                        target.boundingBox,
                        pos,
                        Vec3.ZERO,
                        ExposureSample.getExposureSample(target.bbWidth, target.bbHeight)
                    )
                )
            }
        }

        list.sortBy { player.distanceTo(it.entity) }

        return list.asSequence()
            .filter { it.entity.isAlive }
            .take(maxTargets)
    }

    context (NonNullContext)
    private fun getTargetInfo(entity: LivingEntity, ticks: Int): TargetInfo {
        val motionX = (entity.x - entity.xo).coerceIn(-0.6, 0.6)
        val motionY = (entity.y - entity.yo).coerceIn(-0.5, 0.5)
        val motionZ = (entity.z - entity.zo).coerceIn(-0.6, 0.6)

        val entityBox = entity.boundingBox
        var targetBox = entityBox
        for (tick in 0..ticks) {
            targetBox = canMove(targetBox, motionX, motionY, motionZ)
                ?: canMove(targetBox, motionX, 0.0, motionZ)
                        ?: canMove(targetBox, 0.0, motionY, 0.0)
                        ?: break
        }

        val offsetX = targetBox.minX - entityBox.minX
        val offsetY = targetBox.minY - entityBox.minY
        val offsetZ = targetBox.minZ - entityBox.minZ
        val motion = Vec3(offsetX, offsetY, offsetZ)
        val pos = entity.position()

        return TargetInfo(
            entity,
            pos.add(motion),
            targetBox,
            pos,
            motion,
            ExposureSample.getExposureSample(entity.bbWidth, entity.bbHeight)
        )
    }

    private fun LivingEntity.getMinArmorRate(): Int {
        var minDura = 100

        armorStacksCompat.forEachFast { armor ->
            if (!armor.isDamageableItem) return@forEachFast
            val dura = armor.damageValue
            if (dura < minDura) {
                minDura = dura
            }
        }

        return minDura
    }

    context (NonNullContext)
    private fun shouldForcePlace(entity: LivingEntity): Boolean {
        return (!forcePlaceWhileSwording || !player.mainHandItem.isSwordCompat)
                && (entity.totalHealth <= forcePlaceHealth
                || entity.realSpeed >= forcePlaceMotion
                || entity.getMinArmorRate() <= forcePlaceArmorRate)
    }

    context (NonNullContext)
    private fun canMove(box: AABB, x: Double, y: Double, z: Double): AABB? {
        return box.move(x, y, z).takeIf { !world.checkBlockCollision(it) }
    }

    context (NonNullContext)
    private fun calcPlaceInfo(checkRotation: Boolean): PlaceInfo? {
        var placeInfo: PlaceInfo.Mutable? = null
        val time = measureNanoTime {
            val targets = targets.get(25L).toList()
            if (targets.isEmpty()) return@measureNanoTime

            val context = CombatManager.contextSelf ?: return null

            val mutableBlockPos = BlockPos.MutableBlockPos()
            val targetBlocks = blocks.get(blocksUpdateTime)
            if (targetBlocks.isEmpty()) return@measureNanoTime

            val max = PlaceInfo.Mutable(player)
            val safe = PlaceInfo.Mutable(player)
            val lethal = PlaceInfo.Mutable(player)

            val noSuicide = noSuicide
            val crystals = CombatManager.crystalList

            val damagePriority = damagePriority

            for (pos in targetBlocks) {
                val placeBox = CrystalUtils.getCrystalPlacingBB(pos)

                val crystalX = pos.x + 0.5
                val crystalY = pos.y + 1.0
                val crystalZ = pos.z + 0.5

                val selfDamage = max(
                    context.calcDamage(crystalX, crystalY, crystalZ, false, mutableBlockPos),
                    context.calcDamage(crystalX, crystalY, crystalZ, true, mutableBlockPos)
                )
                val collidingDamage = calcCollidingCrystalDamage(crystals, placeBox)
                val adjustedDamage = max(selfDamage, collidingDamage - collidingCrystalExtraSelfDamageThreshold)

                if (player.scaledHealth - adjustedDamage <= noSuicide) continue
                if (player.scaledHealth - collidingDamage <= noSuicide) continue
                if (!lethalOverride && adjustedDamage > placeMaxSelf) continue

                for ((entity, entityPos, entityBox, currentPos) in targets) {
                    if (entityBox.intersects(placeBox)) continue
                    if (placeBox.intersects(entityPos, currentPos)) continue

                    val targetDamage =
                        calcDamage(entity, entityPos, entityBox, crystalX, crystalY, crystalZ, mutableBlockPos)
                    if (lethalOverride && targetDamage - entity.totalHealth > lethalThresholdAddition && selfDamage < lethal.selfDamage && selfDamage <= lethalMaxSelfDamage) {
                        lethal.update(entity, pos, selfDamage, targetDamage)
                    }

                    if (adjustedDamage > placeMaxSelf) continue

                    val minDamage: Float
                    val balance: Float

                    if (shouldForcePlace(entity)) {
                        minDamage = forcePlaceMinDamage
                        balance = forcePlaceBalance
                    } else {
                        minDamage = placeMinDmg
                        balance = placeBalance
                    }

                    if (targetDamage < minDamage || targetDamage - adjustedDamage < balance) continue

                    if (damagePriority(selfDamage, targetDamage) > damagePriority(max.selfDamage, max.targetDamage)) {
                        max.update(entity, pos, adjustedDamage, targetDamage)
                    } else if (max.targetDamage - targetDamage <= safeMaxTargetDamageReduction
                        && max.selfDamage - adjustedDamage >= safeMinSelfDamageReduction
                    ) {
                        safe.update(entity, pos, adjustedDamage, targetDamage)
                    }
                }
            }

            if (max.targetDamage - safe.targetDamage > safeMaxTargetDamageReduction
                || max.selfDamage - safe.selfDamage <= safeMinSelfDamageReduction
            ) {
                safe.clear(player)
            }

            placeInfo = lethal.takeValid()
                ?: safe.takeValid()
                        ?: max.takeValid()

            placeInfo?.calcPlacement()
        }

        synchronized(calculationTimes) {
            calculationTimesPending.add(time.toInt())
        }

        return placeInfo
    }

    private fun calcCollidingCrystalDamage(
        crystals: List<Pair<EndCrystal, CrystalDamage>>,
        placeBox: AABB
    ): Float {
        var max = 0.0f

        for ((crystal, crystalDamage) in crystals) {
            if (!placeBox.intersects(crystal.boundingBox)) continue
            if (crystalDamage.selfDamage > max) {
                max = crystalDamage.selfDamage
            }
        }

        return max
    }

    context (NonNullContext)
    private fun calcDamage(
        entity: LivingEntity,
        entityPos: Vec3,
        entityBox: AABB,
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Float {
        val isPlayer = entity is Player
        if (isPlayer && world.difficulty == Difficulty.PEACEFUL) return 0.0f
        var damage: Float

        damage = if (isPlayer
            && crystalY - entityPos.y > 1.5652173822904127
            && checkResistant(
                mutableBlockPos.set(
                    crystalX.floorToInt(),
                    crystalY.floorToInt() - 1,
                    crystalZ.floorToInt()
                ),
                world.getBlockState(mutableBlockPos)
            )
        ) {
            1.0f
        } else {
            calcRawDamage(entityPos, entityBox, crystalX, crystalY, crystalZ, mutableBlockPos)
        }

        if (isPlayer) damage = calcDifficultyDamage(world, damage)
        return calcReductionDamage(entity, damage)
    }

    private val reductionMap = Collections.synchronizedMap(WeakHashMap<LivingEntity, DamageReduction>())

    private class DamageReduction(entity: LivingEntity) {
        private val armorValue: Float = entity.armorValue.toFloat()
        private val toughness: Float = entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS).toFloat()
        private val blastReduction: Float = 1.0f - min(calcTotalEPF(entity), 20) / 25.0f
        private val resistance: Float

        init {
            val potionEffect = entity.getEffect(MobEffects.RESISTANCE)
            resistance = if (potionEffect != null) max(1.0f - (potionEffect.amplifier + 1) * 0.2f, 0.0f) else 1.0f
        }

        fun calcReductionDamage(damage: Float): Float {
            return CombatRules.getDamageAfterAbsorb(damage, armorValue, toughness) *
                    resistance *
                    blastReduction
        }

        companion object {
            private fun calcTotalEPF(entity: LivingEntity): Int {
                var epf = 0
                for (itemStack in entity.armorStacksCompat) {
                    val nbtTagList = itemStack.enchantments
                    for (i in 0 until nbtTagList.size()) {
                        val nbtTagCompound = nbtTagList.entrySet().toList()[i]
                        val id = nbtTagCompound.key
                        val level = nbtTagCompound.intValue
                        if (id.unwrapKey().get() == Enchantments.PROTECTION) {
                            // Protection
                            epf += level
                        } else if (id.unwrapKey().get() == Enchantments.BLAST_PROTECTION) {
                            // Blast protection
                            epf += level * 2
                        }
                    }
                }
                return epf
            }
        }
    }

    private const val DOUBLE_SIZE = 12.0f
    private const val DAMAGE_FACTOR = 42.0f

    context (NonNullContext)
    private fun calcRawDamage(
        entityPos: Vec3,
        entityBox: AABB,
        posX: Double,
        posY: Double,
        posZ: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Float {
        val scaledDist = entityPos.distanceTo(posX, posY, posZ).toFloat() / DOUBLE_SIZE
        if (scaledDist > 1.0f) return 0.0f

        val factor = (1.0f - scaledDist) * getExposureAmount(entityBox, posX, posY, posZ, mutableBlockPos)
        return ((factor * factor + factor) * DAMAGE_FACTOR + 1.0f)
    }

    private val function = FastRayTraceFunction { pos, blockState, _ ->
        if (checkResistant(pos, blockState)) {
            FastRayTraceAction.CALC
        } else {
            FastRayTraceAction.SKIP
        }
    }

    private fun checkResistant(pos: BlockPos, state: BlockState): Boolean {
        return CrystalUtils.isResistant(state)
                && (!assumeInstantMine
                || !PacketMine.isMine(pos))
    }

    context (NonNullContext)
    private fun getExposureAmount(
        entityBox: AABB,
        posX: Double,
        posY: Double,
        posZ: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Float {
        val width = entityBox.maxX - entityBox.minX
        val height = entityBox.maxY - entityBox.minY

        val gridMultiplierXZ = 1.0 / (width * 2.0 + 1.0)
        val gridMultiplierY = 1.0 / (height * 2.0 + 1.0)

        val gridXZ = width * gridMultiplierXZ
        val gridY = height * gridMultiplierY

        val sizeXZ = (1.0 / gridMultiplierXZ).floorToInt()
        val sizeY = (1.0 / gridMultiplierY).floorToInt()
        val xzOffset = (1.0 - gridMultiplierXZ * (sizeXZ)) / 2.0

        var total = 0
        var count = 0

        for (yIndex in 0..sizeY) {
            for (xIndex in 0..sizeXZ) {
                for (zIndex in 0..sizeXZ) {
                    val x = gridXZ * xIndex + xzOffset + entityBox.minX
                    val y = gridY * yIndex + entityBox.minY
                    val z = gridXZ * zIndex + xzOffset + entityBox.minZ

                    total++
                    if (!world.fastRayTrace(x, y, z, posX, posY, posZ, 20, mutableBlockPos, function)) {
                        count++
                    }
                }
            }
        }

        return count.toFloat() / total.toFloat()
    }

    private fun calcReductionDamage(entity: LivingEntity, damage: Float): Float {
        val reduction = reductionMap[entity]
        return reduction?.calcReductionDamage(damage) ?: damage
    }

    private fun calcDifficultyDamage(world: ClientLevel, damage: Float): Float {
        return when (world.difficulty) {
            Difficulty.PEACEFUL -> 0.0f
            Difficulty.EASY -> min(damage * 0.5f + 1.0f, damage)
            Difficulty.HARD -> damage * 1.5f
            else -> damage
        }
    }


    private fun checkPlaceCollision(
        pos: BlockPos,
        collidingEntities: List<Entity>
    ): Boolean {
        val minX = pos.x + 0.001
        val minY = pos.y + 1.0
        val minZ = pos.z + 0.001
        val maxX = pos.x + 0.999
        val maxY = pos.y + 3.0
        val maxZ = pos.z + 0.999

        return collidingEntities.none {
            it.boundingBox.intersects(minX, minY, minZ, maxX, maxY, maxZ)
        }
    }

    context (NonNullContext)
    private fun getCollidingEntities(
        rangeSq: Float,
        feetXInt: Int,
        feetYInt: Int,
        feetZInt: Int,
        single: Boolean,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): List<Entity> {
        val collidingEntities = ArrayList<Entity>()
        val rangeSqCeil = rangeSq.ceilToInt()

        for (entity in EntityManager.entity) {
            if (!entity.isAlive) continue

            val adjustedRange = rangeSqCeil - ((entity.bbWidth / 2.0f).sq * 2.0f).ceilToInt()
            val dist = entity.distanceSqTo(BlockPos(feetXInt, feetYInt, feetZInt).center)

            if (dist > adjustedRange) continue

            if (entity !is EndCrystal) {
                collidingEntities.add(entity)
            } else {
                if (!single) {
                    collidingEntities.add(entity)
                } else if (!checkBreakRange(entity, mutableBlockPos)) {
                    collidingEntities.add(entity)
                }
            }
        }

        return collidingEntities
    }

    private fun checkPlaceRotation(pos: BlockPos, eyePos: Vec3, sight: Vec3): Boolean {
        val grow = ClientSettings.placeRotationBoundingBoxGrow
        val growPos = 1.0 + grow
        val bb = AABB(
            pos.x - grow, pos.y - grow, pos.z - grow,
            pos.x + growPos, pos.y + growPos, pos.z + growPos
        )
        if (bb.clip(eyePos, sight).isPresent) return true

        return placeRotationRange != 0.0f
                && checkRotationDiff(RotationUtils.getRotationTo(eyePos, pos.toVec3Center()), placeRotationRange)
    }

    context (NonNullContext)
    private fun checkCrystalRotation(x: Double, y: Double, z: Double): Boolean {
        if (!crystalRotation) return true

        val eyePos = PlayerPacketManager.position.add(0.0, player.eyeY - player.y, 0.0)
        val sight = eyePos.add(PlayerPacketManager.rotation.toViewVec().scale(8.0))

        return checkCrystalRotation(CrystalUtils.getCrystalBB(x, y, z), eyePos, sight)
    }

    private fun checkCrystalRotation(box: AABB, eyePos: Vec3, sight: Vec3): Boolean {
        return !crystalRotation
                || box.clip(eyePos, sight).isPresent
                || breakRotationRange != 0.0f && checkRotationDiff(RotationUtils.getRotationTo(eyePos, box.center), breakRotationRange)
    }

    private fun calcDirection(eyePos: Vec3, hitVec: Vec3): Direction {
        val x = eyePos.x - hitVec.x
        val y = eyePos.y - hitVec.y
        val z = eyePos.z - hitVec.z

        return Direction.entries.maxByOrNull {
            x * it.unitVec3.x + y * it.unitVec3.y + z * it.unitVec3.z
        } ?: Direction.NORTH
    }

    private fun checkRotationDiff(rotation: Vec2f, range: Float): Boolean {
        val serverSide = PlayerPacketManager.rotation
        return RotationUtils.calcAbsAngleDiff(rotation.x, serverSide.x) <= range
                && RotationUtils.calcAbsAngleDiff(rotation.y, serverSide.y) <= range
    }

    context (NonNullContext)
    private fun checkBreakRange(
        entity: EndCrystal,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        return checkBreakRange(entity.x, entity.y, entity.z, mutableBlockPos)
    }

    context (NonNullContext)
    private fun checkBreakRange(
        x: Double,
        y: Double,
        z: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        return player.breakDistanceSq(x, y, z) <= breakRange.sq
                && (player.distanceSqTo(x, y, z) <= wallRange.sq
                || world.rayTraceVisible(
            player.x,
            player.eyeY,
            player.z,
            x,
            y + 1.7,
            z,
            20,
            mutableBlockPos
        ))
    }

    private fun Entity.placeDistanceSq(x: Double, y: Double, z: Double): Double {
        return when (placeRangeMode) {
            RangeMode.Feet -> distanceSqTo(x, y, z)
            RangeMode.Eyes -> eyeDistanceSq(x, y, z)
        }
    }

    private fun Entity.breakDistanceSq(x: Double, y: Double, z: Double): Double {
        return when (breakRangeMode) {
            RangeMode.Feet -> distanceSqTo(x, y, z)
            RangeMode.Eyes -> eyeDistanceSq(x, y, z)
        }
    }

    private fun Entity.eyeDistanceSq(x: Double, y: Double, z: Double): Double {
        return distanceSq(this.x, this.eyeY, this.z, x, y, z)
    }

    private open class PlaceInfo(
        open val target: LivingEntity?,
        open val blockPos: BlockPos,
        open val selfDamage: Float,
        open val targetDamage: Float,
        open val side: Direction,
        open val hitVecOffset: Vec3f,
        open val hitVec: Vec3
    ) {
        class Mutable(
            target: LivingEntity
        ) : PlaceInfo(
            target,
            BlockPos.ZERO,
            Float.MAX_VALUE,
            forcePlaceMinDamage,
            Direction.UP,
            Vec3f.ZERO,
            Vec3.ZERO
        ) {
            override var target = target; private set
            override var blockPos = super.blockPos; private set
            override var selfDamage = super.selfDamage; private set
            override var targetDamage = super.targetDamage; private set
            override var side = super.side; private set
            override var hitVecOffset = super.hitVecOffset; private set
            override var hitVec = super.hitVec; private set

            fun update(
                target: LivingEntity,
                blockPos: BlockPos,
                selfDamage: Float,
                targetDamage: Float
            ) {
                this.target = target
                this.blockPos = blockPos
                this.selfDamage = selfDamage
                this.targetDamage = targetDamage
            }

            fun clear(player: Player) {
                update(player, BlockPos.ZERO, Float.MAX_VALUE, forcePlaceMinDamage)
            }

            context (NonNullContext)
            fun calcPlacement() {
                when (placeBypass) {
                    PlaceBypass.Up -> {
                        side = Direction.UP
                        hitVecOffset = Vec3f(0.5f, 1.0f, 0.5f)
                        hitVec = Vec3(blockPos.x + 0.5, blockPos.y + 1.0, blockPos.z + 0.5)
                    }
                    PlaceBypass.Down -> {
                        side = Direction.DOWN
                        hitVecOffset = Vec3f(0.5f, 0.0f, 0.5f)
                        hitVec = Vec3(blockPos.x + 0.5, blockPos.y.toDouble(), blockPos.z + 0.5)
                    }
                    PlaceBypass.Closest -> {
                        side = getMiningSide(blockPos) ?: calcDirection(player.eyePosition, blockPos.toVec3Center())
                        val directionVec = side.unitVec3i
                        val x = directionVec.x * 0.5f + 0.5f
                        val y = directionVec.y * 0.5f + 0.5f
                        val z = directionVec.z * 0.5f + 0.5f
                        hitVecOffset = Vec3f(x, y, z)
                        hitVec = blockPos.toVec3(x.toDouble(), y.toDouble(), z.toDouble())
                    }
                }
            }

            fun takeValid(): Mutable? {
                return this.takeIf {
                    target != null
                            && target != mc.player
                            && selfDamage != Float.MAX_VALUE
                            && targetDamage != forcePlaceMinDamage
                }
            }
        }



        companion object {
            @JvmField
            val INVALID = PlaceInfo(null, BlockPos.ZERO, Float.NaN, Float.NaN, Direction.UP, Vec3f.ZERO, Vec3.ZERO)
        }

        override fun toString(): String {
            return "PlaceInfo(target=$target, blockPos=$blockPos, selfDamage=$selfDamage, targetDamage=$targetDamage, " +
                    "side=$side, hitVecOffset=$hitVecOffset, hitVec=$hitVec)"
        }
    }

    private open class BreakInfo(
        open val crystal: EndCrystal?,
        open val selfDamage: Float,
        open val targetDamage: Float
    ) {
        class Mutable : BreakInfo(null, Float.MAX_VALUE, forcePlaceMinDamage) {
            override var crystal = super.crystal; private set
            override var selfDamage = super.selfDamage; private set
            override var targetDamage = super.targetDamage; private set

            fun update(
                target: EndCrystal,
                selfDamage: Float,
                targetDamage: Float
            ) {
                this.crystal = target
                this.selfDamage = selfDamage
                this.targetDamage = targetDamage
            }

            fun clear() {
                crystal = null
                selfDamage = Float.MAX_VALUE
                targetDamage = forcePlaceMinDamage
            }
        }

        fun takeValid(): BreakInfo? {
            return this.takeIf {
                crystal != null
                        && selfDamage != Float.MAX_VALUE
                        && targetDamage != forcePlaceMinDamage
            }
        }

        override fun toString(): String {
            return "BreakInfo(crystal=$crystal, selfDamage=$selfDamage, targetDamage=$targetDamage)"
        }
    }

    private data class TargetInfo(
        val entity: LivingEntity,
        val pos: Vec3,
        val box: AABB,
        val currentPos: Vec3,
        val predictMotion: Vec3,
        val exposureSample: ExposureSample
    ) {
        override fun toString(): String {
            return "TargetInfo(entity=$entity, pos=$pos, box=$box, currentPos=$currentPos, " +
                    "predictMotion=$predictMotion, exposureSample=$exposureSample)"
        }
    }

    private enum class Page(override val displayName: CharSequence) : Displayable {
        General("General"),
        Place("Place"),
        Break("Break"),
        ForcePlace("Force Place"),
        Calculation("Calculation"),
        Misc("Miscellaneous"),
        Visual("Visual"),
    }

    private enum class HudInfo(override val displayName: String) : Displayable {
        OFF("Off"),
        SPEED("Speed"),
        TARGET("Target"),
        DAMAGE("Damage"),
        CALCULATION_TIME("Calculation Time")
    }

    private enum class SwingMode : Displayable {
        Client {
            override fun swingHand(event: NonNullContext, hand: InteractionHand) {
                event.player.swing(hand)
            }
        },
        Packet {
            override fun swingHand(event: NonNullContext, hand: InteractionHand) {
                event.netHandler.send(ServerboundSwingPacket(hand))
            }
        };

        abstract fun swingHand(event: NonNullContext, hand: InteractionHand)
    }

    private enum class SwingHand(override val displayName: String) : Displayable {
        Auto("Auto"),
        OffHand("Off Hand"),
        MainHand("Main Hand")
    }

    private enum class SwitchMode(override val displayName: String) : Displayable {
        Off("Off"),
        Legit("Legit"),
        Ghost("Ghost"),
        Inventory("Inventory")
    }

    private enum class PlaceMode : Displayable {
        Single,
        Multi,
        Off
    }

    private enum class BreakMode : Displayable {
        Smart,
        All,
        Off
    }

    private enum class RangeMode : Displayable {
        Eyes,
        Feet
    }

    private enum class PacketPlaceMode(override val displayName: String, val onRemove: Boolean, val onBreak: Boolean) : Displayable {
        Off("Off", false, false),
        Weak("Weak", true, false),
        Strong("Strong", true, true)
    }

    private enum class PlaceBypass(override val displayName: String) : Displayable {
        Up("Up"),
        Down("Down"),
        Closest("Closest")
    }

    private enum class DamagePriority(override val displayName: String) : Displayable {
        Efficient("Efficient") {
            override operator fun invoke(selfDamage: Float, targetDamage: Float): Float {
                return targetDamage - selfDamage
            }
        },
        Aggressive("Aggressive") {
            override operator fun invoke(selfDamage: Float, targetDamage: Float): Float {
                return targetDamage
            }
        };

        abstract operator fun invoke(selfDamage: Float, targetDamage: Float): Float
    }

    private object Renderer {
        @JvmField
        var lastBlockPos: BlockPos? = null

        @JvmField
        var prevPos: Vec3? = null

        @JvmField
        var currentPos: Vec3? = null

        @JvmField
        var lastRenderPos: Vec3? = null

        @JvmField
        var lastUpdateTime = 0L

        @JvmField
        var startTime = 0L

        @JvmField
        var scale = 0.0f

        @JvmField
        var lastSelfDamage = 0.0f

        @JvmField
        var lastTargetDamage = 0.0f

        fun reset() {
            lastBlockPos = null
            prevPos = null
            currentPos = null
            lastRenderPos = null
            lastUpdateTime = 0L
            startTime = 0L
            scale = 0.0f
            lastSelfDamage = 0.0f
            lastTargetDamage = 0.0f
        }

        context(NonNullContext)
        fun onRender3D() {
            val filled = filledAlpha > 0
            val outline = outlineAlpha > 0
            val flag = filled || outline

            if (flag || targetDamage || selfDamage) {
                val placeInfo = renderPlaceInfo
                update(placeInfo)

                prevPos?.let { prevPos ->
                    currentPos?.let { currentPos ->
                        val multiplier = Easing.OUT_QUART.inc(Easing.toDelta(lastUpdateTime, movingLength))
                        val renderPos = prevPos.add(currentPos.subtract(prevPos).scale(multiplier.toDouble()))
                        scale = if (placeInfo != null) {
                            Easing.OUT_CUBIC.inc(Easing.toDelta(startTime, fadeLength))
                        } else {
                            Easing.IN_CUBIC.dec(Easing.toDelta(startTime, fadeLength))
                        }

                        val box = toRenderBox(renderPos, scale)
                        val renderer = ESPRenderer()

                        renderer.aFilled = (filledAlpha * scale).toInt()
                        renderer.aOutline = (outlineAlpha * scale).toInt()
                        renderer.add(box, Colors.color)
                        renderer.render(false)

                        lastRenderPos = renderPos
                    }
                }
            }
        }

        context(NonNullContext)
        fun onRender2D() {
            if (scale != 0.0f && (targetDamage || selfDamage)) {
                lastRenderPos?.let {
                    val text = buildString {
                        if (targetDamage) append("%.1f".format(lastTargetDamage))
                        if (selfDamage) {
                            if (this.isNotEmpty()) append('/')
                            append("%.1f".format(lastSelfDamage))
                        }
                    }

                    val camera = mc.entityRenderDispatcher.camera ?: return@let
                    if (RotationUtils.getRotationDiff(
                            Vec2f(camera.yRot(), camera.xRot()),
                            RotationUtils.getRotationTo(it)) < mc.options.fov().get()) {
                        val screenPos = Render3DUtils.worldToScreen(it)
                        val alpha = (255.0f * scale).toInt()
                        val color = if (scale == 1.0f) ColorRGBA(255, 255, 255) else ColorRGBA(255, 255, 255, alpha)

                        UnicodeFontManager.CURRENT_FONT.drawString(
                            text,
                            screenPos.x.toFloat() - UnicodeFontManager.CURRENT_FONT.getWidth(text, 2.0f) * 0.5f,
                            screenPos.y.toFloat() - UnicodeFontManager.CURRENT_FONT.getHeight(2.0f) * 0.5f,
                            color.awt,
                            2.0f
                        )
                    }
                }
            }
        }

        private fun toRenderBox(Vec3: Vec3, scale: Float): AABB {
            val halfSize = 0.5 * scale
            return AABB(
                Vec3.x - halfSize, Vec3.y - halfSize, Vec3.z - halfSize,
                Vec3.x + halfSize, Vec3.y + halfSize, Vec3.z + halfSize
            )
        }

        private fun update(placeInfo: PlaceInfo?) {
            val newBlockPos = placeInfo?.blockPos
            if (newBlockPos != lastBlockPos) {
                if (placeInfo != null) {
                    currentPos = placeInfo.blockPos.toVec3Center()
                    prevPos = lastRenderPos ?: currentPos
                    lastUpdateTime = System.currentTimeMillis()
                    if (lastBlockPos == null) startTime = System.currentTimeMillis()
                } else {
                    lastUpdateTime = System.currentTimeMillis()
                    if (lastBlockPos != null) startTime = System.currentTimeMillis()
                }

                lastBlockPos = newBlockPos
            }

            if (placeInfo != null) {
                lastSelfDamage = placeInfo.selfDamage
                lastTargetDamage = placeInfo.targetDamage
            }
        }
    }
}
