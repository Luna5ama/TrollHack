package dev.luna5ama.trollhack.manager.managers

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.api.nonNullParallelHandler
import dev.luna5ama.trollhack.event.impl.LoopEvent
import dev.luna5ama.trollhack.event.impl.player.PlayerPopEvent
import dev.luna5ama.trollhack.manager.AbstractManager
import dev.luna5ama.trollhack.utils.extension.actuallyDead
import dev.luna5ama.trollhack.utils.extension.getOrAddDefault
import dev.luna5ama.trollhack.utils.extension.realHealth
import dev.luna5ama.trollhack.utils.timing.TickTimer
import net.minecraft.world.entity.player.Player
import java.util.concurrent.atomic.AtomicInteger

object PlayerPopTotemManager : AbstractManager(), AlwaysListening {
    private val popMap = Object2ObjectMaps.synchronize(Object2ObjectLinkedOpenHashMap<Player, PopTracker>())

    init {
        nonNullHandler<PlayerPopEvent> {
            val tracker = popMap.getOrAddDefault(it.player, PopTracker())
            tracker.pop()
        }

        nonNullParallelHandler<LoopEvent.Tick> {
            EntityManager.players.forEach {
                popMap.getOrAddDefault(it, PopTracker())
                if (it.isDeadOrDying) popMap.getOrAddDefault(it, PopTracker()).reset()
            }
        }
    }

    fun getPopCount(player: Player) = popMap.getOrAddDefault(player, PopTracker()).popCount.get()

    fun canPop(player: Player, damage: Double) = popMap.getOrAddDefault(player, PopTracker()).canPop() && player.realHealth < damage

    fun lastPop(player: Player) = popMap.getOrDefault(player, PopTracker()).lastPop()

    private class PopTracker {
        val popCount = AtomicInteger(0)
        val popTimer = TickTimer()

        fun pop() {
            popCount.incrementAndGet()
            popTimer.reset()
        }

        fun canPop(): Boolean {
            return popTimer.tick(500)
        }

        fun lastPop() = System.currentTimeMillis() - popTimer.time

        fun reset() {
            popTimer.reset()
            popCount.set(0)
        }
    }
}