package dev.luna5ama.trollhack.module.modules.misc

import dev.luna5ama.trollhack.event.events.GuiEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.WaypointManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.InfoCalculator
import dev.luna5ama.trollhack.util.math.CoordinateConverter.asString
import dev.luna5ama.trollhack.util.text.MessageSendUtils
import net.minecraft.client.gui.GuiGameOver

internal object AutoRespawn : Module(
    name = "Auto Respawn",
    description = "Automatically respawn after dying",
    category = Category.MISC
) {
    private val respawn by setting("Respawn", true)
    private val deathCoords by setting("Save Death Coords", true)
    private val antiGlitchScreen by setting("Anti Glitch Screen", true)

    init {
        safeListener<GuiEvent.Displayed> {
            if (it.screen !is GuiGameOver) return@safeListener

            if (deathCoords && player.health <= 0.0f) {
                WaypointManager.add("Death - " + InfoCalculator.getServerType())
                MessageSendUtils.sendChatMessage("You died at ${player.position.asString()}")
            }

            if (respawn || antiGlitchScreen && player.health > 0.0f) {
                player.respawnPlayer()
                it.screen = null
            }
        }
    }
}