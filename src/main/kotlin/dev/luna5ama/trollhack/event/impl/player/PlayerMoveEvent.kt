package dev.luna5ama.trollhack.event.impl.player

import dev.luna5ama.trollhack.event.api.*
import dev.luna5ama.trollhack.utils.extension.velocityX
import dev.luna5ama.trollhack.utils.extension.velocityY
import dev.luna5ama.trollhack.utils.extension.velocityZ
import net.minecraft.client.player.LocalPlayer

sealed class PlayerMoveEvent : IEvent {
    class Pre(private val player: LocalPlayer) : PlayerMoveEvent(), ICancellable by Cancellable(),
        IPosting by Companion {
        private val prevX = player.velocityX
        private val prevY = player.velocityY
        private val prevZ = player.velocityZ

        val isModified: Boolean
            get() = x != prevX
                || y != prevY
                || z != prevZ

        var x = Double.NaN
            get() = get(field, player.velocityX)

        var y = Double.NaN
            get() = get(field, player.velocityY)

        var z = Double.NaN
            get() = get(field, player.velocityZ)

        private fun get(x: Double, y: Double): Double {
            return when {
                cancelled -> 0.0
                !x.isNaN() -> x
                else -> y
            }
        }

        companion object : NamedProfilerEventBus("nullPlayerMove")
    }

    object Post : PlayerMoveEvent(), IPosting by EventBus()
}