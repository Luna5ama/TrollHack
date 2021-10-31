package cum.xiaro.trollhack.module.modules.misc

import cum.xiaro.trollhack.event.events.GuiEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.WaypointManager
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.InfoCalculator
import cum.xiaro.trollhack.util.math.CoordinateConverter.asString
import cum.xiaro.trollhack.util.text.MessageSendUtils
import net.minecraft.client.gui.GuiGameOver

internal object AutoRespawn : Module(
    name = "AutoRespawn",
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