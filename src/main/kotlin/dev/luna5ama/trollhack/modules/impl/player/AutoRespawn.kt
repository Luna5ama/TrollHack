package dev.luna5ama.trollhack.modules.impl.player

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.UpdateEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import net.minecraft.client.gui.screens.DeathScreen

object AutoRespawn : Module(
    "Auto Respawn", "Automatically respawns when you die.", Category.PLAYER
) {
    init {
        nonNullHandler<UpdateEvent> {
            if (mc.screen is DeathScreen) {
                player.respawn()
                mc.setScreen(null)
            }
        }
    }
}