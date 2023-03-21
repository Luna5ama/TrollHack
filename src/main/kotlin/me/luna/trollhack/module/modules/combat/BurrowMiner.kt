package me.luna.trollhack.module.modules.combat

import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.event.events.TickEvent
import me.luna.trollhack.event.safeConcurrentListener
import me.luna.trollhack.manager.managers.CombatManager
import me.luna.trollhack.manager.managers.HoleManager
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.module.modules.player.PacketMine
import me.luna.trollhack.util.EntityUtils.betterPosition
import me.luna.trollhack.util.TickTimer
import me.luna.trollhack.util.extension.fastFloor
import me.luna.trollhack.util.math.vector.distanceSq
import me.luna.trollhack.util.math.vector.toBlockPos
import me.luna.trollhack.util.math.xCenter
import me.luna.trollhack.util.math.zCenter
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.math.BlockPos

@CombatManager.CombatModule
internal object BurrowMiner : Module(
    name = "BurrowMiner",
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
        val pos = BlockPos.MutableBlockPos()
        val detectBB = target.entityBoundingBox.setMaxY(target.posY + 1.0)

        var minDist = Double.MAX_VALUE
        var minDistPos: BlockPos? = null

        val y = target.posY.fastFloor()
        for (x in detectBB.minX.fastFloor()..detectBB.maxX.fastFloor()) {
            for (z in detectBB.minZ.fastFloor()..detectBB.maxZ.fastFloor()) {
                val dist = distanceSq(x + 0.5, z + 0.5, target.posX, target.posZ)

                if (dist < minDist && !world.isAirBlock(pos.setPos(x, y, z))) {
                    minDist = dist
                    minDistPos = pos
                }
            }
        }

        return minDistPos
    }

    private fun reset() {
        PacketMine.reset(this)
    }
}