package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.movement.ElytraFlight
import dev.luna5ama.trollhack.module.modules.movement.ElytraFlightNew
import dev.luna5ama.trollhack.util.accessor.onGround
import dev.luna5ama.trollhack.util.atValue
import dev.luna5ama.trollhack.util.world.getGroundLevel
import net.minecraft.network.play.client.CPacketPlayer

internal object NoFall : Module(
    name = "No Fall",
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

        safeParallelListener<TickEvent.Post> {
            if (mode.value == Mode.CATCH && noFallCheck() && fallDistCheck()) {
                player.fallDistance = 0.0f
                connection.sendPacket(CPacketPlayer.Position(player.posX, player.posY + 10.0, player.posZ, false))
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
        return !voidOnly.value && player.fallDistance >= distance.value || player.posY < 1 && world.getGroundLevel(
            player
        ) == Double.MIN_VALUE
    }
}