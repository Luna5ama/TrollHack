package dev.luna5ama.trollhack.command.commands

import dev.luna5ama.trollhack.command.ClientCommand

object RotateCommand : ClientCommand(
    name = "rotate",
    description = "Rotate view to specific value."
) {
    init {
        literal("yaw") {
            float("yaw") { yawArg ->
                executeSafe {
                    player.rotationYaw = yawArg.value
                }
            }
        }

        literal("pitch") {
            float("pitch") { pitchArg ->
                executeSafe {
                    player.rotationPitch = pitchArg.value.coerceIn(-90.0f, 90.0f)
                }
            }
        }

        float("yaw") { yawArg ->
            float("pitch") { pitchArg ->
                executeSafe {
                    player.rotationYaw = yawArg.value
                    player.rotationPitch = pitchArg.value.coerceIn(-90.0f, 90.0f)
                }
            }
        }
    }
}