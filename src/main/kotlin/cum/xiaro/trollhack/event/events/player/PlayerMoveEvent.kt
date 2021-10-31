package cum.xiaro.trollhack.event.events.player

import cum.xiaro.trollhack.event.*
import net.minecraft.client.entity.EntityPlayerSP

sealed class PlayerMoveEvent : Event {
    class Pre(private val player: EntityPlayerSP) : PlayerMoveEvent(), ICancellable by Cancellable(), EventPosting by Companion {
        private val prevX = player.motionX
        private val prevY = player.motionY
        private val prevZ = player.motionZ

        val isModified: Boolean
            get() = x != prevX
                || y != prevY
                || z != prevZ

        var x = Double.NaN
            get() = get(field, player.motionX)

        var y = Double.NaN
            get() = get(field, player.motionY)

        var z = Double.NaN
            get() = get(field, player.motionZ)

        @Suppress("NOTHING_TO_INLINE")
        private inline fun get(x: Double, y: Double): Double {
            return when {
                cancelled -> 0.0
                !x.isNaN() -> x
                else -> y
            }
        }

        companion object : NamedProfilerEventBus("trollPlayerMove")
    }

    object Post : PlayerMoveEvent(), EventPosting by EventBus()
}