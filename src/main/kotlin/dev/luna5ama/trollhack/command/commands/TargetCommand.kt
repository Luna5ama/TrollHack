package dev.luna5ama.trollhack.command.commands

import dev.luna5ama.trollhack.command.ClientCommand
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import java.lang.ref.WeakReference

object TargetCommand : ClientCommand(
    name = "target",
    alias = arrayOf(),
    description = "Override combat target"
) {
    init {
        player("player") { playerArg ->
            executeSafe {
                val targetPlayer = playerArg.value
                if (targetPlayer.name == player.name) {
                    NoSpamMessage.sendError(TargetCommand, "You can't target yourself!")
                    return@executeSafe
                }

                val target = world.getPlayerEntityByName(targetPlayer.name)
                if (target == null) {
                    NoSpamMessage.sendError(TargetCommand, "Player ${targetPlayer.name} not found!")
                    return@executeSafe
                }

                CombatManager.targetOverride = WeakReference(target)
                NoSpamMessage.sendMessage("Targeting ${targetPlayer.name}")
            }
        }

        executeSafe {
            CombatManager.targetOverride = null
            NoSpamMessage.sendMessage("Target override cleared")
        }
    }
}