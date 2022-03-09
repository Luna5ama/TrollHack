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
import net.minecraft.util.math.BlockPos

@CombatManager.CombatModule
internal object HoleMiner : Module(
    name = "HoleMiner",
    category = Category.COMBAT,
    description = "Mines your opponent's hole",
    modulePriority = 80
) {
    private val timer = TickTimer()
    private var lastPos: BlockPos? = null

    init {
        onEnable {
            PacketMine.enable()
        }

        onDisable {
            lastPos = null
            timer.reset(-69420L)
            reset()
        }

        safeConcurrentListener<TickEvent.Post> {
            CombatManager.target?.let { target ->
                val pos = target.betterPosition
                if (shouldMineBurrow(pos)) {
                    PacketMine.mineBlock(HoleMiner, pos)
                    timer.reset()
                    lastPos = pos
                } else if (pos != lastPos || timer.tick(5000L)) {
                    reset()
                }
            } ?: reset()
        }
    }

    private fun SafeClientEvent.shouldMineBurrow(pos: BlockPos): Boolean {
        return world.getBlockState(pos).getCollisionBoundingBox(world, pos) != null
            || HoleManager.getHoleInfo(pos).isHole
    }

    private fun reset() {
        PacketMine.reset(this)
    }
}