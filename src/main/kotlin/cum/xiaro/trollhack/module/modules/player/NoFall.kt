package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.player.PlayerMoveEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.module.modules.movement.ElytraFlight
import cum.xiaro.trollhack.module.modules.movement.ElytraFlightNew
import cum.xiaro.trollhack.util.accessor.onGround
import cum.xiaro.trollhack.util.atValue
import cum.xiaro.trollhack.util.world.getGroundLevel
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