package dev.luna5ama.trollhack.module.modules.client

import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.gui.hudgui.elements.client.Notification
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.player.InventorySync
import dev.luna5ama.trollhack.util.TickTimer
import dev.luna5ama.trollhack.util.TimeUnit
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.text.format
import net.minecraft.util.text.TextFormatting

internal object Bypass : Module(
    name = "Bypass",
    category = Category.CLIENT,
    description = "Configures bypass for anticheats",
    visible = false,
    alwaysEnabled = true
) {
    val placeRotationBoundingBoxGrow by setting("Place Rotation Bounding Box Grow", 0.1, 0.0..1.0, 0.01)
    val ghostSwitchBypass by setting("Ghost Switch Bypass", HotbarSwitchManager.BypassMode.NONE)

    private val swapWarningMessage =
        "$chatName ${TextFormatting.RED format "You're using swap mode ghost switch bypass,it is recommended to have Inventory Sync enabled."}"

    private val warningTimer = TickTimer(TimeUnit.SECONDS)

    init {
        safeParallelListener<TickEvent.Post> {
            if (ghostSwitchBypass == HotbarSwitchManager.BypassMode.SWAP && InventorySync.isDisabled && warningTimer.tick(5)) {
                val id = (Bypass.hashCode().toLong() shl 32) or swapWarningMessage.hashCode().toLong()
                NoSpamMessage.sendWarning(id, swapWarningMessage)
            }
        }
    }
}