package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import net.minecraft.network.play.client.CPacketAnimation

internal object HandSwing : Module(
    name = "Hand Swing",
    category = Category.PLAYER,
    description = "Modifies hand swing animation"
) {
    private val cancelClientSide by setting("Cancel Client Side", false)
    private val cancelServerSide by setting("Cancel Server Side", false)
    val swingTicks by setting("Swing ticks", -1, -1..20, 1, { !cancelClientSide })
    val cancelEquipAnimation by setting("Cancel Equip Animation", false)

    val modifiedSwingSpeed: Boolean
        get() = isEnabled && !cancelClientSide && swingTicks != -1

    init {
        listener<PacketEvent.Send> {
            if (cancelServerSide && it.packet is CPacketAnimation) it.cancel()
        }

        safeListener<RunGameLoopEvent.Render> {
            if (cancelClientSide) {
                player.isSwingInProgress = false
                player.swingProgressInt = 0
                player.swingProgress = 0.0f
                player.prevSwingProgress = 0.0f
            }
        }
    }
}