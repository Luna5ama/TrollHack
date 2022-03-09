package me.luna.trollhack.module.modules.misc

import me.luna.trollhack.event.events.TickEvent
import me.luna.trollhack.event.safeParallelListener
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.interfaces.DisplayEnum
import me.luna.trollhack.util.threads.runSafe
import net.minecraft.world.GameType

internal object FakeGameMode : Module(
    name = "FakeGameMode",
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