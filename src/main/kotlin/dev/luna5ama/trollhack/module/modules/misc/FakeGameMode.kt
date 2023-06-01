package dev.luna5ama.trollhack.module.modules.misc

import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.threads.runSafe
import net.minecraft.world.GameType

internal object FakeGameMode : Module(
    name = "Fake Game Mode",
    description = "Fakes your current gamemode client side",
    category = Category.MISC
) {
    private val gamemode by setting("Mode", GameMode.CREATIVE)

    @Suppress("UNUSED")
    private enum class GameMode(override val displayName: CharSequence, val gameType: GameType) : DisplayEnum {
        SURVIVAL("Survival", GameType.SURVIVAL),
        CREATIVE("Creative", GameType.CREATIVE),
        ADVENTURE("Adventure", GameType.ADVENTURE),
        SPECTATOR("Spectator", GameType.SPECTATOR)
    }

    override fun getHudInfo(): String {
        return gamemode.displayString
    }

    private var prevGameMode: GameType? = null

    init {
        safeParallelListener<TickEvent.Pre> {
            playerController.setGameType(gamemode.gameType)
        }

        onEnable {
            runSafe {
                prevGameMode = playerController.currentGameType
            } ?: disable()
        }

        onDisable {
            runSafe {
                prevGameMode?.let { playerController.setGameType(it) }
            }
        }
    }
}