package me.luna.trollhack.module.modules.player

import me.luna.trollhack.event.events.PacketEvent
import me.luna.trollhack.event.events.RunGameLoopEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.SwingMode
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