package me.luna.trollhack.command.commands

import me.luna.trollhack.command.ClientCommand
import net.minecraft.entity.Entity
import net.minecraft.network.play.client.CPacketPlayer

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