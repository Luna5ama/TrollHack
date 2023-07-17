package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.distanceSq
import dev.fastmc.common.floorToInt
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeConcurrentListener
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.HoleManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.exploit.Burrow
import dev.luna5ama.trollhack.module.modules.player.PacketMine
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.world.isAir
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.math.BlockPos

@CombatManager.CombatModule
internal object BurrowMiner : Module(
    name = "Burrow Miner",
    category = Category.COMBAT,
    description = "Mines your opponent's burrow"
) {
    private val mode by setting("Mode", Mode.BURROW)
    private val timeout by setting("Timeout", 5000, 0..10000, 100)

    private var lastPos: BlockPos? = null
    private val timeoutTimer = TickTimer()

    private enum class Mode {
        BURROW, CORNER
    }

    init {
        onEnable {
            PacketMine.enable()
        }

        onDisable {
            lastPos = null
            timeoutTimer.reset(-69420L)
            reset()
        }

        safeConcurrentListener<TickEvent.Post> {
            CombatManager.target?.let { target ->
                when (mode) {
                    Mode.BURROW -> mineBurrow(target)
                    Mode.CORNER -> mineCorner(target)
                }
            } ?: reset()
        }
    }

    private fun SafeClientEvent.mineBurrow(target: EntityLivingBase) {
        val pos = target.betterPosition
        if (pos == player.betterPosition) return
        val burrow = Burrow.isBurrowed(target)
        val isHole = HoleManager.getHoleInfo(pos).isHole

        if (burrow || isHole) {
            val priority = if (burrow || pos == lastPos) 80 else -100
            PacketMine.mineBlock(BurrowMiner, pos, priority)
            timeoutTimer.reset()
            lastPos = pos
        } else if (pos != lastPos || timeoutTimer.tick(timeout)) {
            reset()
        }
    }

    private fun SafeClientEvent.mineCorner(target: EntityLivingBase) {
        val pos = getClipPos(target)

        if (pos != null) {
            PacketMine.mineBlock(BurrowMiner, pos, 80)
            timeoutTimer.reset()
            lastPos = pos
        } else if (timeoutTimer.tick(timeout)) {
            reset()
        }
    }

    private fun SafeClientEvent.getClipPos(target: EntityLivingBase): BlockPos? {
        val detectBB = target.entityBoundingBox

        var minDist = Double.MAX_VALUE
        val minDistPos = BlockPos.MutableBlockPos()

        val y = target.posY.floorToInt()
        for (x in (detectBB.minX + 0.001).floorToInt()..(detectBB.maxX + 0.001).floorToInt()) {
            for (z in (detectBB.minZ + 0.001).floorToInt()..(detectBB.maxZ + 0.001).floorToInt()) {
                val dist = distanceSq(x + 0.5, z + 0.5, target.posX, target.posZ)

                if (dist < minDist && !world.isAir(x, y, z)) {
                    minDist = dist
                    minDistPos.setPos(x, y, z)
                }
            }
        }

        return minDistPos.takeIf { minDist < Double.MAX_VALUE }
    }

    private fun reset() {
        PacketMine.reset(this)
    }
}