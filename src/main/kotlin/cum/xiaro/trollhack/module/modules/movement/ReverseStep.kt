package cum.xiaro.trollhack.module.modules.movement

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.player.PlayerTravelEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.EntityUtils.isInOrAboveLiquid
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.world.getGroundLevel
import net.minecraft.network.play.server.SPacketPlayerPosLook

internal object ReverseStep : Module(
    name = "ReverseStep",
    description = "Walks down edge of block faster",
    category = Category.MOVEMENT
) {
    private val height by setting("Height", 2.0f, 0.25f..8.0f, 0.1f)
    private val speed by setting("Speed", 1.0f, 0.1f..8.0f, 0.1f)

    private val timer = TickTimer()

    init {
        listener<PacketEvent.Receive> {
            if (it.packet is SPacketPlayerPosLook) {
                timer.reset()
            }
        }

        safeListener<PlayerTravelEvent>(100) {
            if (shouldRun()) {
                player.motionY -= speed.toDouble()
                Speed.resetReverseStep()
            }
        }
    }

    private fun SafeClientEvent.shouldRun(): Boolean {
        return !mc.gameSettings.keyBindSneak.isKeyDown
            && !mc.gameSettings.keyBindJump.isKeyDown
            && !player.isElytraFlying
            && !player.capabilities.isFlying
            && !player.isOnLadder
            && !player.isInOrAboveLiquid
            && player.onGround
            && player.motionY in -0.08..0.0
            && timer.tick(3000L)
            && (player.posY - world.getGroundLevel(player) in 0.25..height.toDouble()
            || player.posY - world.getGroundLevel(player.entityBoundingBox.offset(player.motionX, 0.0, player.motionZ)) in 0.25..height.toDouble())
    }
}