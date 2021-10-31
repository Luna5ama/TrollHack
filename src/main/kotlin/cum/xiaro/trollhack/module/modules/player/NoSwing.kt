package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.RunGameLoopEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.SwingMode
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