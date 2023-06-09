package dev.luna5ama.trollhack.module.modules.movement

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.player.PlayerTravelEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.isInOrAboveLiquid
import dev.luna5ama.trollhack.util.world.getGroundLevel
import net.minecraft.network.play.server.SPacketPlayerPosLook

internal object ReverseStep : Module(
    name = "Reverse Step",
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
            && checkGroundLevel()
    }

    private fun SafeClientEvent.checkGroundLevel(): Boolean {
        return player.posY - world.getGroundLevel(player) in 0.25..height.toDouble()
            || player.posY - world.getGroundLevel(
            player.entityBoundingBox.offset(
                player.motionX,
                0.0,
                player.motionZ
            )
        ) in 0.25..height.toDouble()
            || player.posY - world.getGroundLevel(
            player.entityBoundingBox.offset(
                player.motionX * 2.0,
                0.0,
                player.motionZ * 2.0
            )
        ) in 0.25..height.toDouble()
    }
}