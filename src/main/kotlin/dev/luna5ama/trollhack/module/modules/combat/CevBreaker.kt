package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSetDeadEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSpawnEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.graphics.ESPRenderer
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.InventoryTaskManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.exploit.Burrow
import dev.luna5ama.trollhack.module.modules.player.PacketMine
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.accessor.id
import dev.luna5ama.trollhack.util.accessor.packetAction
import dev.luna5ama.trollhack.util.combat.CrystalUtils
import dev.luna5ama.trollhack.util.combat.CrystalUtils.hasValidSpaceForCrystal
import dev.luna5ama.trollhack.util.inventory.slot.allSlotsPrioritized
import dev.luna5ama.trollhack.util.inventory.slot.firstBlock
import dev.luna5ama.trollhack.util.inventory.slot.firstItem
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.math.vector.distanceSqToCenter
import dev.luna5ama.trollhack.util.math.vector.toVec3d
import dev.luna5ama.trollhack.util.world.*
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

@CombatManager.CombatModule
internal object CevBreaker : Module(
    name = "Cev Breaker",
    description = "Troll module",
    category = Category.COMBAT,
    modulePriority = 400
) {
    private val minHealth by setting("Min Health", 8.0f, 0.0f..20.0f, 0.5f)
    private val placeDelay by setting("Place Delay", 500, 0..1000, 1)
    private val breakDelay by setting("Break Delay", 100, 0..1000, 1)
    private val range by setting("Range", 5.0f, 0.0f..6.0f, 0.25f)

    private val renderer = ESPRenderer().apply { aFilled = 31; aOutline = 233 }
    private val placeTimer = TickTimer()
    private val breakTimer = TickTimer()
    private val packetTimer = TickTimer()
    private var posInfo: Info? = null
    private var crystalID = -69420

    init {
        onEnable {
            PacketMine.enable()
        }

        onDisable {
            reset()
        }

        listener<Render3DEvent> {
            posInfo?.let {
                renderer.render(false)
            }
        }

        safeListener<WorldEvent.ServerBlockUpdate> { event ->
            val info = posInfo ?: return@safeListener

            if (event.pos == info.pos) {
                val current = event.oldState.block
                val new = event.newState.block

                if (new != current && new == Blocks.AIR) {
                    val id = crystalID
                    if (id != -69420 && safeCheck()) {
                        breakCrystal(id)
                    }
                }
            }
        }

        safeListener<PacketEvent.Receive> { event ->
            if (event.packet !is SPacketSoundEffect) return@safeListener
            val info = posInfo ?: return@safeListener

            if (event.packet.category == SoundCategory.BLOCKS
                && event.packet.sound == SoundEvents.ENTITY_GENERIC_EXPLODE
                && info.pos.distanceSqToCenter(event.packet.x, event.packet.y - 0.5, event.packet.z) < 0.25
            ) {
                crystalID = -69420
            }
        }

        safeListener<CrystalSpawnEvent> {
            val info = posInfo ?: return@safeListener
            if (it.crystalDamage.blockPos != info.pos
                && !CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(info.pos, it.crystalDamage.crystalPos)
            ) return@safeListener

            crystalID = it.entityID
        }

        safeListener<CrystalSetDeadEvent> { event ->
            val info = posInfo ?: return@safeListener
            if (event.crystals.none { it.entityBoundingBox.intersects(info.placeBB) }) return@safeListener

            crystalID = -69420
            place(info)
        }

        safeParallelListener<TickEvent.Post> {
            if (!safeCheck()) {
                posInfo = null
                return@safeParallelListener
            }

            updateTarget()
        }

        safeListener<RunGameLoopEvent.Tick> {
            if (!safeCheck()) {
                return@safeListener
            }

            val info = posInfo ?: return@safeListener
            val blockState = world.getBlockState(info.pos)

            if (blockState.block == Blocks.AIR) {
                var id = crystalID
                if (id == -69420) {
                    CombatManager.crystalList.asSequence()
                        .filter { !it.first.isDead }
                        .find { it.first.entityBoundingBox.intersects(info.placeBB) }
                        ?.let { id = it.first.entityId }
                }

                PacketMine.mineBlock(CevBreaker, info.pos, CevBreaker.modulePriority)

                if (id != -69420) {
                    if (breakTimer.tick(breakDelay)) breakCrystal(id)
                }

                if (placeTimer.tick(placeDelay)) place(info)
            }
        }
    }

    private fun SafeClientEvent.safeCheck(): Boolean {
        return player.health >= minHealth
            && AutoOffhand.lastType != AutoOffhand.Type.TOTEM
    }

    private fun SafeClientEvent.updateTarget() {
        val targetInfo = calcTarget()

        if (targetInfo == null) {
            reset()
        } else if (targetInfo != posInfo) {
            reset()

            renderer.clear()
            renderer.add(AxisAlignedBB(targetInfo.pos), ColorRGB(255, 255, 255))
            packetTimer.reset(-69420)

            PacketMine.mineBlock(CevBreaker, targetInfo.pos, CevBreaker.modulePriority)
        }

        posInfo = targetInfo
    }

    private fun SafeClientEvent.calcTarget(): Info? {
        val target = CombatManager.target ?: return null
        if (Burrow.isBurrowed(target)) return null

        val pos = BlockPos(target.posX, target.posY + 2.5, target.posZ)

        if (player.distanceSqToCenter(pos) > range * range) return null
        if (!world.canBreakBlock(pos)) return null
        if (!hasValidSpaceForCrystal(pos)) return null
        if (!wallCheck(pos)) return null

        return Info(pos, getMiningSide(pos) ?: EnumFacing.UP)
    }

    private fun SafeClientEvent.wallCheck(pos: BlockPos): Boolean {
        val eyePos = player.eyePosition
        return eyePos.distanceSqTo(pos.x + 0.5, pos.y + 1.0, pos.z + 0.5) <= 9.0
            || world.rayTraceBlocks(eyePos, pos.toVec3d(0.5, 2.7, 0.5), false, true, false) == null
    }

    private fun SafeClientEvent.place(info: Info) {
        val obsidianSlot = player.allSlotsPrioritized.firstBlock(Blocks.OBSIDIAN) ?: return
        val crystalSlot = player.allSlotsPrioritized.firstItem(Items.END_CRYSTAL) ?: return
        val placeInfo = getPlacement(
            info.pos,
            arrayOf(*EnumFacing.HORIZONTALS, EnumFacing.DOWN),
            PlacementSearchOption.ENTITY_COLLISION,
            PlacementSearchOption.range(6.0f)
        ) ?: return

        synchronized(InventoryTaskManager) {
            ghostSwitch(obsidianSlot) {
                placeBlock(placeInfo)
            }

            ghostSwitch(crystalSlot) {
                connection.sendPacket(
                    CPacketPlayerTryUseItemOnBlock(
                        info.pos,
                        info.side,
                        EnumHand.MAIN_HAND,
                        info.hitVecOffset.x,
                        info.hitVecOffset.y,
                        info.hitVecOffset.z
                    )
                )

                connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
            }
        }

        placeTimer.reset()
    }

    private fun SafeClientEvent.breakCrystal(id: Int) {
        val packet = CPacketUseEntity().apply {
            this.id = id
            this.packetAction = CPacketUseEntity.Action.ATTACK
        }
        connection.sendPacket(packet)
        connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
        breakTimer.reset()
    }

    private fun reset() {
        placeTimer.reset(-69420)
        breakTimer.reset(-69420)
        packetTimer.reset(-69420)
        posInfo = null
        crystalID = -69420
        PacketMine.reset(this)
    }

    private class Info(
        val pos: BlockPos,
        val side: EnumFacing,
    ) {
        val placeBB = AxisAlignedBB(
            pos.x - 1.0, pos.y + 0.0, pos.z - 1.0,
            pos.x + 2.0, pos.y + 3.0, pos.z + 2.0
        )
        val hitVecOffset = getHitVecOffset(side)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Info) return false

            return pos == other.pos
        }

        override fun hashCode(): Int {
            return pos.hashCode()
        }
    }
}