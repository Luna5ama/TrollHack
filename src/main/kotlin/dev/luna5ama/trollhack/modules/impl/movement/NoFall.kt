package dev.luna5ama.trollhack.modules.impl.movement

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.TickEvent
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

object NoFall : Module("No Fall", "Prevents fall damage.", Category.MOVEMENT) {
    private val mode by setting("Mode", Mode.GROUND_SPOOF)
    private val fallDistance by setting("Fall Distance", 3.0, 3.0..16.0, 1.0, { mode == Mode.GROUND_SPOOF })

    init {
        nonNullHandler<TickEvent.Pre> {
            if (!isFalling()) return@nonNullHandler

            when (mode) {
                Mode.GROUND_SPOOF -> sendPlayerPacket { onGround(true) }
                Mode.GRIM_2B2T -> {
                    netHandler.send(
                        ServerboundMovePlayerPacket.PosRot(
                            player.x,
                            player.y + 1.0E-9,
                            player.z,
                            player.yRot,
                            player.xRot,
                            false,
                            player.horizontalCollision
                        )
                    )
                    player.resetFallDistance()
                }
            }
        }
    }

    private fun isFalling(): Boolean {
        val player = mc.player ?: return false
        if (player.isFallFlying) return false
        return player.fallDistance > if (mode == Mode.GRIM_2B2T) 3.0f else fallDistance.toFloat()
    }

    private enum class Mode(override val displayName: CharSequence) : Displayable {
        GROUND_SPOOF("GroundSpoof"),
        GRIM_2B2T("Grim2B2T")
    }
}
