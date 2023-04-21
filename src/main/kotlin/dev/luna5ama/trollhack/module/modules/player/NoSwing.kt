package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.SwingMode
import net.minecraft.network.play.client.CPacketAnimation

internal object NoSwing : Module(
    name = "NoSwing",
    category = Category.PLAYER,
    description = "Cancels server or client swing animation"
) {
    private val mode by setting("Mode", SwingMode.CLIENT)

    init {
        listener<PacketEvent.Send> {
            if (mode == SwingMode.PACKET && it.packet is CPacketAnimation) it.cancel()
        }

        safeListener<RunGameLoopEvent.Render> {
            player.isSwingInProgress = false
            player.swingProgressInt = 0
            player.swingProgress = 0.0f
            player.prevSwingProgress = 0.0f
        }
    }
}