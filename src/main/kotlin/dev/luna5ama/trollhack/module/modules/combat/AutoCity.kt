package dev.luna5ama.trollhack.module.modules.combat

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSetDeadEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSpawnEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.HoleManager
import dev.luna5ama.trollhack.manager.managers.HotbarManager.spoofHotbar
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.player.PacketMine
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.EntityUtils.spoofSneak
import dev.luna5ama.trollhack.util.TickTimer
import dev.luna5ama.trollhack.util.accessor.id
import dev.luna5ama.trollhack.util.accessor.packetAction
import dev.luna5ama.trollhack.util.combat.CalcContext
import dev.luna5ama.trollhack.util.combat.CrystalUtils
import dev.luna5ama.trollhack.util.combat.CrystalUtils.canPlaceCrystal
import dev.luna5ama.trollhack.util.combat.CrystalUtils.canPlaceCrystalOn
import dev.luna5ama.trollhack.util.combat.CrystalUtils.isValidMaterial
import dev.luna5ama.trollhack.util.extension.sq
import dev.luna5ama.trollhack.util.inventory.slot.firstBlock
import dev.luna5ama.trollhack.util.inventory.slot.firstItem
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import dev.luna5ama.trollhack.util.math.VectorUtils.setAndAdd
import dev.luna5ama.trollhack.util.world.FastRayTraceAction
import dev.luna5ama.trollhack.util.world.fastRaytrace
import dev.luna5ama.trollhack.util.world.getBlock
import dev.luna5ama.trollhack.util.world.isReplaceable
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos

@CombatManager.CombatModule
internal object AutoCity : Module(
    name = "Auto City",
    category = Category.COMBAT,
    description = "Trolling",
    modulePriority = 100
) {
    private val placeAnvil by setting("Place Anvil", false)
    private val placeCrystal by setting("Place Crystal", true)
    private val breakCrystal by setting("Break Crystal", true)
    private val placeDelay by setting("Place Delay", 100, 0..1000, 5)
    private val breakDelay by setting("Break Delay", 100, 0..1000, 5)
    private val packetBreakDelay by setting("Packet Break Delay", 100, 0..1000, 5)
    private val minDamage by setting("Min Damage", 4.0f, 1.0f..10.0f, 0.1f)
    private val minUpdateDamage by setting("Min Update Damage", 8.0f, 1.0f..10.0f, 0.1f)
    private val updateDelay by setting("Update Delay", 200, 0..1000, 5)
    private val range by setting("Range", 4.5f, 1.0f..6.0f, 0.1f)

    private val placeTimer = TickTimer()
    private val breakTimer = TickTimer()
    private val updateTimer = TickTimer()
    private val packetBreakTimer = TickTimer()

    private var anvilPos: BlockPos? = null
    private var crystalPos: BlockPos? = null
    private var holePos: BlockPos? = null

    private var anvilPlaced = false
    private var crystalID = -1

    override fun isActive(): Boolean {
        return isEnabled && (anvilPos != null && (placeAnvil || placeCrystal || breakCrystal))
    }

    init {
        onDisable {
            placeTimer.reset(-114514L)
            breakTimer.reset(-114514L)

            anvilPos = null
            crystalPos = null
            holePos = null

            anvilPlaced = false
            crystalID = -1
        }

        safeParallelListener<TickEvent.Post> {
            updateTargetPos()

            val anvilPos = anvilPos
            val crystalPos = crystalPos

            if (anvilPos == null || crystalPos == null) {
                PacketMine.reset(AutoCity)
                return@safeParallelListener
            }

            val posUp = anvilPos.up()
            val block = world.getBlock(posUp)

            anvilPlaced = block != Blocks.AIR

            crystalID = CombatManager.crystalList
                .find {
                    !it.first.isDead
                        && CrystalUtils.crystalIntersects(it.second.crystalPos, crystalPos)
                }?.first?.entityId ?: -1

            if (block != Blocks.AIR) {
                PacketMine.mineBlock(AutoCity, posUp, AutoCity.modulePriority)
            }
        }

        safeListener<CrystalSpawnEvent> {
            val anvilPos = anvilPos ?: return@safeListener
            val crystalPos = crystalPos ?: return@safeListener

            if (CrystalUtils.crystalIntersects(crystalPos, it.crystalDamage.blockPos)) {
                crystalID = it.entityID
                if (!anvilPlaced && breakTimer.tick(0)) {
                    breakCrystal(it.entityID)
                    placeAnvil(anvilPos)
                    placeCrystal(crystalPos)
                }
            }
        }

        safeListener<WorldEvent.ServerBlockUpdate> {
            val entityID = crystalID

            val anvilPos = anvilPos ?: return@safeListener
            val crystalPos = crystalPos ?: return@safeListener
            val posUp = anvilPos.up()

            if (it.pos != posUp) return@safeListener

            if (it.newState.block == Blocks.AIR) {
                anvilPlaced = false

                if (entityID != -1
                    && packetBreakTimer.tick(packetBreakDelay)
                    && breakTimer.tick(0)
                ) {
                    breakCrystal(entityID)
                    placeAnvil(anvilPos)
                    placeCrystal(crystalPos)
                    packetBreakTimer.reset()
                }
            } else {
                PacketMine.mineBlock(AutoCity, posUp, AutoCity.modulePriority)
            }
        }

        safeListener<CrystalSetDeadEvent> { event ->
            val anvilPos = anvilPos ?: return@safeListener
            val crystalPos = crystalPos ?: return@safeListener
            if (event.crystals.none { CrystalUtils.crystalIntersects(it, crystalPos) }) return@safeListener
            crystalID = -1

            placeAnvil(anvilPos)
            placeCrystal(crystalPos)
        }

        safeListener<RunGameLoopEvent.Tick> {
            val anvilPos = anvilPos ?: return@safeListener
            val crystalPos = crystalPos ?: return@safeListener
            val entityID = crystalID

            if (!anvilPlaced && entityID != -1 && breakTimer.tick(breakDelay)) {
                breakCrystal(entityID)
                placeAnvil(anvilPos)
                placeCrystal(crystalPos)

                placeTimer.reset()
            }

            if (placeTimer.tick(placeDelay)) {
                if (!anvilPlaced) {
                    placeAnvil(anvilPos)
                } else {
                    val anvilPosUp = anvilPos.up()
                    if (world.getBlock(anvilPosUp) != Blocks.ANVIL
                        && world.getBlockState(anvilPosUp.up()).isReplaceable
                    ) {
                        placeAnvil(anvilPosUp)
                    }
                }

                if (entityID == -1) {
                    placeCrystal(crystalPos)
                }
            }
        }
    }

    private fun SafeClientEvent.updateTargetPos() {
        val flag = anvilPos != null
        val target = CombatManager.target

        if (target != null) {
            val targetPos = target.betterPosition

            val result = if (flag || targetPos == holePos || HoleManager.getHoleInfo(target).isHole) {
                val mutableBlockPos = BlockPos.MutableBlockPos()

                val anvilPos = anvilPos
                val crystalPos = crystalPos

                CombatManager.contextTarget?.let { context ->
                    if (anvilPos != null && crystalPos != null) {
                        val damage = calcDamage(context, anvilPos.up(), crystalPos, mutableBlockPos)
                        if (damage > minUpdateDamage && !updateTimer.tick(updateDelay)) return
                    }

                    var maxDamage = minDamage
                    var result: Pair<BlockPos, BlockPos>? = null

                    for (pair in calcSequence(targetPos)) {
                        val damage = calcDamage(context, pair.first, pair.second, mutableBlockPos)

                        if (damage > maxDamage) {
                            maxDamage = damage
                            result = pair
                        }
                    }

                    if (result == null) {
                        for (pair in calcInPlaceSequence(targetPos)) {
                            val damage = calcDamage(context, pair.first, pair.second, mutableBlockPos)

                            if (damage > maxDamage) {
                                maxDamage = damage
                                result = pair
                            }
                        }
                    }

                    val prevAnvil = this@AutoCity.anvilPos
                    val prevAnvilUp = this@AutoCity.anvilPos?.up()
                    val prevCrystal = this@AutoCity.crystalPos

                    if (result != null
                        && prevAnvil != null && prevAnvilUp != null && prevCrystal != null
                        && (result.first != prevAnvilUp || result.second != prevCrystal)
                    ) {
                        val blockState = world.getBlockState(prevAnvil)
                        if ((blockState.block == Blocks.ANVIL || !CrystalUtils.isResistant(blockState))
                            || maxDamage - calcDamage(context, prevAnvilUp, prevCrystal, mutableBlockPos) < 2.0f
                        ) {
                            result = prevAnvilUp to prevCrystal
                        }
                    }


                    result
                }
            } else {
                null
            }

            if (result != null) {
                anvilPos = result.first.down()
                crystalPos = result.second
                holePos = target.betterPosition
                return
            }
        }

        anvilPos = null
        crystalPos = null
        holePos = null
    }

    private fun SafeClientEvent.calcSequence(
        targetPos: BlockPos,
    ): Sequence<Pair<BlockPos, BlockPos>> {
        val playerPos = player.betterPosition
        val eyePos = player.eyePosition
        val rangeSq = range.sq
        val mutableBlockPos = BlockPos.MutableBlockPos()

        return EnumFacing.HORIZONTALS.asSequence()
            .flatMap { mainSide ->
                val opposite = mainSide.opposite
                val pos1 = targetPos.offset(mainSide)
                EnumFacing.HORIZONTALS.asSequence()
                    .filter {
                        it != opposite
                    }.map {
                        pos1 to pos1.offset(it).down()
                    }
            }.filter { (minePos, _) ->
                playerPos.distanceSq(minePos) <= rangeSq
            }
            .filter { (minePos, crystalPos) ->
                val dist = playerPos.distanceSq(crystalPos)
                dist <= rangeSq
                    && (dist <= 9
                    || !world.fastRaytrace(
                    eyePos,
                    crystalPos.x + 0.5,
                    crystalPos.y + 2.7,
                    crystalPos.z + 0.5,
                    20,
                    mutableBlockPos
                ) { rayTracePos, blockState ->
                    if (rayTracePos == minePos || blockState.getCollisionBoundingBox(this, rayTracePos) != null) {
                        FastRayTraceAction.CALC
                    } else {
                        FastRayTraceAction.SKIP
                    }
                })
            }
            .filter { (minePos, crystalPos) ->
                world.getBlock(minePos) != Blocks.BEDROCK
                    && canPlaceCrystal(crystalPos, mutableBlockPos = mutableBlockPos)
            }
    }

    private fun SafeClientEvent.calcInPlaceSequence(
        targetPos: BlockPos,
    ): Sequence<Pair<BlockPos, BlockPos>> {
        val playerPos = player.betterPosition
        val eyePos = player.eyePosition
        val rangeSq = range.sq
        val mutableBlockPos = BlockPos.MutableBlockPos()

        return EnumFacing.HORIZONTALS.asSequence()
            .map { mainSide ->
                val surroundPos = targetPos.offset(mainSide)
                surroundPos to surroundPos.down()
            }.filter { (minePos, _) ->
                playerPos.distanceSq(minePos) <= rangeSq
            }
            .filter { (minePos, crystalPos) ->
                val dist = playerPos.distanceSq(crystalPos)
                dist <= rangeSq
                    && (dist <= 9
                    || !world.fastRaytrace(
                    eyePos,
                    crystalPos.x + 0.5,
                    crystalPos.y + 2.7,
                    crystalPos.z + 0.5,
                    20,
                    mutableBlockPos
                ) { rayTracePos, blockState ->
                    if (rayTracePos == minePos || blockState.getCollisionBoundingBox(this, rayTracePos) != null) {
                        FastRayTraceAction.CALC
                    } else {
                        FastRayTraceAction.SKIP
                    }
                })
            }
            .filter { (minePos, crystalPos) ->
                world.getBlock(minePos) != Blocks.BEDROCK
                    && canPlaceCrystalOn(crystalPos)
            }
            .filter { (_, crystalPos) ->
                CombatSetting.newCrystalPlacement
                    || isValidMaterial(world.getBlockState(mutableBlockPos.setAndAdd(crystalPos, 0, 2, 0)))
            }
    }


    private fun calcDamage(
        context: CalcContext,
        minePos: BlockPos,
        pos: BlockPos,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Float {
        return context.calcDamage(
            pos.x + 0.5,
            pos.y + 1.0,
            pos.z + 0.5,
            false,
            6.0f,
            mutableBlockPos
        ) { rayTracePos, blockState ->
            if (rayTracePos != minePos && blockState.block != Blocks.AIR && CrystalUtils.isResistant(blockState)) {
                FastRayTraceAction.CALC
            } else {
                FastRayTraceAction.SKIP
            }
        }
    }

    private fun SafeClientEvent.placeCrystal(targetPos: BlockPos) {
        if (!placeCrystal) return

        player.hotbarSlots.firstItem(Items.END_CRYSTAL)?.let {
            spoofHotbar(it) {
                connection.sendPacket(
                    CPacketPlayerTryUseItemOnBlock(
                        targetPos,
                        EnumFacing.UP,
                        EnumHand.MAIN_HAND,
                        0.5f,
                        1.0f,
                        0.5f
                    )
                )
            }
            connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
            placeTimer.reset()
        }
    }

    private fun SafeClientEvent.placeAnvil(targetPos: BlockPos) {
        if (!placeAnvil) return
        player.hotbarSlots.firstBlock(Blocks.ANVIL)?.let {
            player.spoofSneak {
                spoofHotbar(it) {
                    connection.sendPacket(
                        CPacketPlayerTryUseItemOnBlock(
                            targetPos,
                            EnumFacing.UP,
                            EnumHand.MAIN_HAND,
                            0.5f,
                            1.0f,
                            0.5f
                        )
                    )
                }
            }
            connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
            placeTimer.reset()
        }
    }

    private fun SafeClientEvent.breakCrystal(entityID: Int) {
        if (!breakCrystal) return

        connection.sendPacket(
            CPacketUseEntity().apply {
                packetAction = CPacketUseEntity.Action.ATTACK
                id = entityID
            }
        )
        connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
        breakTimer.reset()
    }
}