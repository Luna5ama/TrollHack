package cum.xiaro.trollhack.module.modules.combat

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.safeConcurrentListener
import cum.xiaro.trollhack.manager.managers.CombatManager
import cum.xiaro.trollhack.manager.managers.HoleManager
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.module.modules.player.PacketMine
import cum.xiaro.trollhack.util.EntityUtils.betterPosition
import cum.xiaro.trollhack.util.TickTimer
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