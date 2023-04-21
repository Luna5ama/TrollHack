package dev.luna5ama.trollhack.command.commands

import dev.luna5ama.trollhack.command.ClientCommand

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