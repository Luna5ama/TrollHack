package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.distance
import dev.fastmc.common.sq
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeConcurrentListener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.ESPRenderer
import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.HoleManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.EntityUtils.isFriend
import dev.luna5ama.trollhack.util.EntityUtils.isSelf
import dev.luna5ama.trollhack.util.EntityUtils.spoofSneak
import dev.luna5ama.trollhack.util.collections.asSequenceFast
import dev.luna5ama.trollhack.util.collections.forEachFast
import dev.luna5ama.trollhack.util.combat.HoleType
import dev.luna5ama.trollhack.util.inventory.slot.allSlotsPrioritized
import dev.luna5ama.trollhack.util.inventory.slot.firstBlock
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.math.isInSight
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.math.vector.distanceSqToCenter
import dev.luna5ama.trollhack.util.math.vector.toVec3d
import dev.luna5ama.trollhack.util.runIf
import dev.luna5ama.trollhack.util.threads.onMainThread
import dev.luna5ama.trollhack.util.threads.runSynchronized
import dev.luna5ama.trollhack.util.world.isReplaceable
import it.unimi.dsi.fastutil.longs.Long2LongMaps
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.objects.Object2LongMaps
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.inventory.Slot
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

internal object AutoHoleFill : Module(
    name = "Auto Hole Fill",
    description = "Automatically fills hole while enemy walks into hole",
    category = Category.COMBAT,
    modulePriority = 100
) {
    private val bedrockHole by setting("Bedrock Hole", true)
    private val obbyHole by setting("Obby Hole", true)
    private val twoBlocksHole by setting("2 Blocks Hole", true)
    private val fourBlocksHole by setting("4 Blocks Hole", true)
    private val targetHole by setting("Target Hole", false)
    private val predictTicks by setting("Predict Ticks", 8, 0..50, 1)
    private val detectRange by setting("Detect Range", 5.0f, 0.0f..16.0f, 0.25f)
    private val hRange by setting("H Range", 0.5f, 0.0f..4.0f, 0.1f)
    private val vRange by setting("V Range", 3.0f, 0.0f..8.0f, 0.1f)
    private val distanceBalance by setting("Distance Balance", 1.0f, -5.0f..5.0f, 0.1f)
    private val fillDelay by setting("Fill Delay", 50, 0..1000, 10)
    private val fillTimeout by setting("Fill Timeout", 100, 0..1000, 10)
    private val fillRange by setting("Fill Range", 5.0f, 1.0f..6.0f, 0.1f)
    private val targetColor by setting("Target Color", ColorRGB(32, 255, 32), false)
    private val otherColor by setting("Other Color", ColorRGB(255, 222, 32), false)
    private val filledColor by setting("Filled Color", ColorRGB(255, 32, 32), false)

    private val placeMap = Long2LongMaps.synchronize(Long2LongOpenHashMap())
    private val updateTimer = TickTimer()
    private val placeTimer = TickTimer()

    private var holeInfos = emptyList<IntermediateHoleInfo>()
    private var nextHole: BlockPos? = null
    private val renderBlockMap = Object2LongMaps.synchronize(Object2LongOpenHashMap<BlockPos>())
    private val renderer = ESPRenderer().apply { aFilled = 33; aOutline = 233 }

    init {
        onDisable {
            holeInfos = emptyList()
            nextHole = null
            renderBlockMap.clear()
            renderer.replaceAll(mutableListOf())
        }

        listener<WorldEvent.ClientBlockUpdate> {
            if (!it.newState.isReplaceable) {
                placeMap.remove(it.pos.toLong())
                if (it.pos == nextHole) nextHole = null
                renderBlockMap.runSynchronized {
                    replace(it.pos, System.currentTimeMillis())
                }
            }
        }

        listener<Render3DEvent> {
            renderBlockMap.runSynchronized {
                object2LongEntrySet().forEach {
                    val color = when {
                        it.key == nextHole -> targetColor
                        it.longValue == -1L -> otherColor
                        else -> filledColor
                    }

                    if (it.longValue == -1L) {
                        ESPRenderer.Info(it.key, color)
                    } else {
                        val progress = Easing.IN_CUBIC.dec(Easing.toDelta(it.longValue, 1000L))
                        val size = progress * 0.5
                        val n = 0.5 - size
                        val p = 0.5 + size
                        val box = AxisAlignedBB(
                            it.key.x + n, it.key.y + n, it.key.z + n,
                            it.key.x + p, it.key.y + p, it.key.z + p,
                        )
                        renderer.add(box, color.alpha((255.0f * progress).toInt()))
                    }
                }
            }

            renderer.render(true)
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (Bypass.blockPlaceRotation) {
                (nextHole ?: getRotationPos(holeInfos))?.let {
                    sendPlayerPacket {
                        rotate(getRotationTo(it.toVec3d(0.5, -0.5, 0.5)))
                    }
                }
            }
        }

        safeConcurrentListener<RunGameLoopEvent.Tick> {
            val slot = player.allSlotsPrioritized.firstBlock(Blocks.OBSIDIAN)
            val place = placeTimer.tick(fillDelay) && slot != null

            if (place || updateTimer.tickAndReset(5L)) {
                val newHoleInfo = getHoleInfos()
                holeInfos = newHoleInfo

                val current = System.currentTimeMillis()
                placeMap.runSynchronized {
                    values.removeIf { it <= current }
                    nextHole?.let {
                        if (!containsKey(it.toLong())) nextHole = null
                    }
                }

                if (place) {
                    getPos(newHoleInfo, Bypass.blockPlaceRotation)?.let {
                        nextHole = it
                        placeBlock(slot!!, it)
                    }
                } else {
                    updatePosRender(newHoleInfo)
                }
            }
        }
    }

    private fun SafeClientEvent.updatePosRender(holeInfos: List<IntermediateHoleInfo>) {
        val set = LongOpenHashSet()

        calcPosSequence(holeInfos).forEach {
            set.add(it.second.blockPos.toLong())
            renderBlockMap.putIfAbsent(it.second.blockPos, -1L)
        }

        renderBlockMap.runSynchronized {
            object2LongEntrySet().removeIf {
                it.longValue == -1L && !placeMap.containsKey(it.key.toLong()) && !set.contains(it.key.toLong())
            }
        }
    }

    private fun SafeClientEvent.getPos(holeInfos: List<IntermediateHoleInfo>, checkRotation: Boolean): BlockPos? {
        val set = LongOpenHashSet()
        val eyePos = PlayerPacketManager.eyePosition

        val targetPos = calcPosSequence(holeInfos).onEach {
            set.add(it.second.blockPos.toLong())
            renderBlockMap.putIfAbsent(it.second.blockPos, -1L)
        }.runIf(checkRotation) {
            filter { (_, holeInfo) ->
                val pos = holeInfo.blockPos
                AxisAlignedBB(
                    pos.x.toDouble(), pos.y - 1.0, pos.z.toDouble(),
                    pos.x + 1.0, pos.y.toDouble(), pos.z + 1.0,
                ).isInSight(eyePos, PlayerPacketManager.rotation)
            }
        }.minByOrNull { it.first.horizontalDist(it.second.center) }?.second?.blockPos

        renderBlockMap.runSynchronized {
            object2LongEntrySet().removeIf {
                it.longValue == -1L && !placeMap.containsKey(it.key.toLong()) && !set.contains(it.key.toLong())
            }
        }

        return targetPos
    }

    private fun SafeClientEvent.calcPosSequence(holeInfos: List<IntermediateHoleInfo>): Sequence<Pair<EntityPlayer, IntermediateHoleInfo>> {
        return sequence {
            val detectRangeSq = detectRange.sq

            EntityManager.players.forEachFast outer@{ entity ->
                if (entity.isSelf) return@outer
                if (!entity.isEntityAlive) return@outer
                if (entity.isFriend) return@outer
                if (player.distanceSqTo(entity) > detectRangeSq) return@outer

                val current = entity.positionVector
                val predict = entity.calcPredict(current)

                holeInfos.forEachFast { holeInfo ->
                    if (entity.posY <= holeInfo.blockPos.y + 0.5) return@forEachFast
                    val dist = entity.horizontalDist(holeInfo.center)
                    if (holeInfo.toward && holeInfo.playerDist - dist < distanceBalance) return@forEachFast
                    if (!holeInfo.detectBox.contains(current)
                        && (holeInfo.toward || !holeInfo.detectBox.intersects(current, predict))
                    ) return@forEachFast

                    yield(entity to holeInfo)
                }
            }
        }
    }

    private fun SafeClientEvent.getRotationPos(holeInfos: List<IntermediateHoleInfo>): BlockPos? {
        return calcPosSequence(holeInfos)
            .minByOrNull { it.first.horizontalDist(it.second.center) }
            ?.second?.blockPos
    }

    private fun SafeClientEvent.getHoleInfos(): List<IntermediateHoleInfo> {
        val eyePos = player.eyePosition
        val rangeSq = fillRange.sq
        val entities = EntityManager.entity.filter {
            it.preventEntitySpawning && it.isEntityAlive
        }

        return HoleManager.holeInfos.asSequenceFast()
            .filterNot {
                it.isFullyTrapped
            }
            .filter {
                when (it.type) {
                    HoleType.BEDROCK -> bedrockHole
                    HoleType.OBBY -> obbyHole
                    HoleType.TWO -> twoBlocksHole
                    HoleType.FOUR -> fourBlocksHole
                    else -> false
                }
            }
            .filter {
                targetHole || (it.origin != HoleSnap.hole?.origin && it.origin != HolePathFinder.hole?.origin)
            }
            .filterNot {
                player.entityBoundingBox.intersects(it.boundingBox)
            }
            .filter { holeInfo ->
                entities.none {
                    it.entityBoundingBox.intersects(holeInfo.boundingBox)
                }
            }
            .flatMap { holeInfo ->
                holeInfo.holePos.asSequence()
                    .filter { !placeMap.containsKey(it.toLong()) }
                    .filter { eyePos.distanceSqToCenter(it) <= rangeSq }
                    .filter { world.getBlockState(it).isReplaceable }
                    .map {
                        val box = AxisAlignedBB(
                            holeInfo.boundingBox.minX - hRange,
                            holeInfo.boundingBox.minY,
                            holeInfo.boundingBox.minZ - hRange,
                            holeInfo.boundingBox.maxX + hRange,
                            holeInfo.boundingBox.maxY + vRange,
                            holeInfo.boundingBox.maxZ + hRange
                        )
                        val dist = player.horizontalDist(holeInfo.center)
                        val prevDist =
                            distance(player.lastTickPosX, player.lastTickPosZ, holeInfo.center.x, holeInfo.center.z)
                        IntermediateHoleInfo(
                            holeInfo.center,
                            it,
                            box,
                            dist,
                            holeInfo.origin == HoleSnap.hole?.origin || dist - prevDist < -0.15
                        )
                    }
            }
            .toList()
    }

    private fun Entity.horizontalDist(vec3d: Vec3d): Double {
        return distance(this.posX, this.posZ, vec3d.x, vec3d.z)
    }

    private fun Entity.calcPredict(current: Vec3d): Vec3d {
        return if (predictTicks == 0) {
            current
        } else {
            Vec3d(
                this.posX + (this.posX - this.lastTickPosX) * predictTicks,
                this.posY + (this.posY - this.lastTickPosY) * predictTicks,
                this.posZ + (this.posZ - this.lastTickPosZ) * predictTicks
            )
        }
    }

    private fun SafeClientEvent.placeBlock(slot: Slot, pos: BlockPos) {
        val target = pos.down()

        val packet = CPacketPlayerTryUseItemOnBlock(target, EnumFacing.UP, EnumHand.MAIN_HAND, 0.5f, 1.0f, 0.5f)

        onMainThread {
            player.spoofSneak {
                ghostSwitch(slot) {
                    connection.sendPacket(packet)
                }
            }

            connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
        }

        placeMap[pos.toLong()] = System.currentTimeMillis() + fillTimeout
        placeTimer.reset()
    }

    private class IntermediateHoleInfo(
        val center: Vec3d,
        val blockPos: BlockPos,
        val detectBox: AxisAlignedBB,
        val playerDist: Double,
        val toward: Boolean
    )
}