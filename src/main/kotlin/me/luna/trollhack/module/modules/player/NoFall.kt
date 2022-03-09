package me.luna.trollhack.module.modules.player

import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.event.events.PacketEvent
import me.luna.trollhack.event.events.player.PlayerMoveEvent
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.module.modules.movement.ElytraFlight
import me.luna.trollhack.module.modules.movement.ElytraFlightNew
import me.luna.trollhack.util.accessor.onGround
import me.luna.trollhack.util.atValue
import me.luna.trollhack.util.world.getGroundLevel
import net.minecraft.network.play.client.CPacketPlayer

internal object NoFall : Module(
    name = "NoFall",
    category = Category.PLAYER,
    description = "Prevents fall damage"
) {
    private val distance = setting("Distance", 3, 1..10, 1)
    private val mode = setting("Mode", Mode.CATCH)
    private val voidOnly = setting("Void Only", false, mode.atValue(Mode.CATCH))

    private enum class Mode {
        FALL, CATCH
    }

    init {
        safeListener<PacketEvent.Send> {
            if (it.packet !is CPacketPlayer || !noFallCheck()) return@safeListener
            it.packet.onGround = true
        }

        safeListener<PlayerMoveEvent.Pre> {
            if (mode.value == Mode.CATCH && noFallCheck() && fallDistCheck()) {
                it.y = 10.0
            }
        }
    }

    private fun SafeClientEvent.noFallCheck(): Boolean {
        return !player.isCreative
            && !player.isSpectator
            && !player.isElytraFlying
            && !ElytraFlightNew.isActive()
            && !ElytraFlight.isActive()
    }

    private fun SafeClientEvent.fallDistCheck(): Boolean {
        return !voidOnly.value && player.fallDistance >= distance.value || world.getGroundLevel(player) == -69420.0
    }
}