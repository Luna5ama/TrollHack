package me.luna.trollhack.command.commands

import me.luna.trollhack.command.ClientCommand

object FixSoundCommand : ClientCommand(
    name = "fixsound",
    description = "Fix sound device switching"
) {
    init {
        executeSafe {
            mc.soundHandler.onResourceManagerReload(mc.resourceManager)
        }
    }
}