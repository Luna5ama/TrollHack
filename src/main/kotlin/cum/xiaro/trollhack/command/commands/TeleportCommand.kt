package cum.xiaro.trollhack.command.commands

import cum.xiaro.trollhack.command.ClientCommand
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.util.math.RotationUtils.getRotationTo
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.math.Vec3d
import kotlin.math.max
import kotlin.math.min

object TeleportCommand : ClientCommand(
    name = "teleport",
    alias = arrayOf("tp"),
    description = "Teleport exploit from popbob"
) {
    init {
        blockPos("pos") { posArg ->
            double("speed") { speedArg ->
                executeSafe {
                    val blockPos = posArg.value
                    teleport(blockPos.x, blockPos.y, blockPos.z, speedArg.value)
                }
            }

            executeSafe {
                val blockPos = posArg.value
                teleport(blockPos.x, blockPos.y, blockPos.z)
            }
        }

        int("x") { xArg ->
            int("y") { yArg ->
                int("z") { zArg ->
                    double("speed") { speedArg ->
                        executeSafe {
                            teleport(xArg.value, yArg.value, zArg.value, speedArg.value)
                        }
                    }

                    executeSafe {
                        teleport(xArg.value, yArg.value, zArg.value)
                    }
                }
            }
        }

        double("x") { xArg ->
            double("y") { yArg ->
                double("z") { zArg ->
                    double("speed") { speedArg ->
                        executeSafe {
                            teleport(xArg.value, yArg.value, zArg.value, speedArg.value)
                        }
                    }

                    executeSafe {
                        teleport(xArg.value, yArg.value, zArg.value)
                    }
                }
            }
        }

        int("x") { xArg ->
            int("z") { zArg ->
                double("speed") { speedArg ->
                    executeSafe {
                        teleport(xArg.value + 0.5, player.posY, zArg.value + 0.5, speedArg.value)
                    }
                }

                executeSafe {
                    teleport(xArg.value + 0.5, player.posY, zArg.value + 0.5)
                }
            }
        }

        double("x") { xArg ->
            double("z") { zArg ->
                double("speed") { speedArg ->
                    executeSafe {
                        teleport(xArg.value, player.posY, zArg.value, speedArg.value)
                    }
                }

                executeSafe {
                    teleport(xArg.value, player.posY, zArg.value)
                }
            }
        }
    }

    private fun SafeClientEvent.teleport(x: Int, y: Int, z: Int) {
        teleport(x + 0.5, y.toDouble(), z + 0.5)
    }

    private fun SafeClientEvent.teleport(x: Double, y: Double, z: Double) {
        player.setPosition(x, y, z)
        connection.sendPacket(CPacketPlayer.Position(x, y, z, player.onGround))
    }

    private fun SafeClientEvent.teleport(x: Int, y: Int, z: Int, speed: Double) {
        teleport(x + 0.5, y.toDouble(), z + 0.5, speed)
    }

    private fun SafeClientEvent.teleport(x: Double, y: Double, z: Double, speed: Double) {
        if (speed.isNaN()) {
            player.setPosition(x, y, z)
            connection.sendPacket(CPacketPlayer.Position(x, y, z, player.onGround))
        } else {
            val rotation = getRotationTo(player.positionVector, Vec3d(x, y, z))
            val dirVec = Vec3d.fromPitchYaw(rotation.y, rotation.x).scale(speed)

            var currentX = player.posX
            var currentY = player.posY
            var currentZ = player.posZ

            val xGreater = x >= currentX
            val yGreater = y >= currentY
            val zGreater = z >= currentZ

            do {
                currentX = addOrSubtractBounded(xGreater, currentX, dirVec.x, x)
                currentY = addOrSubtractBounded(yGreater, currentY, dirVec.y, y)
                currentZ = addOrSubtractBounded(zGreater, currentZ, dirVec.z, z)

                player.setPosition(currentX, currentY, currentZ)
                connection.sendPacket(CPacketPlayer.Position(currentX, currentY, currentZ, player.onGround))
            } while (currentX != x || currentY != y || currentZ != z)
        }
    }

    private fun addOrSubtractBounded(greater: Boolean, a: Double, b: Double, bound: Double): Double {
        return if (greater) {
            min(a + b, bound)
        } else {
            max(a + b, bound)
        }
    }
}