package dev.luna5ama.trollhack.modules.impl.combat

import io.netty.buffer.Unpooled
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

object Criticals : Module("Criticals", category = Category.COMBAT) {
    private val legit by setting("Legit", false)

    init {
        nonNullHandler<PacketEvent.Send> {
            if (MaceSpoof.isEnabled && !legit) return@nonNullHandler

            if (it.packet is ServerboundInteractPacket) {
                val playerInteractPacket = it.packet
                val packetBuf = FriendlyByteBuf(Unpooled.buffer())
                playerInteractPacket.write(packetBuf)
                packetBuf.readVarInt()
                val type = packetBuf.readEnum(ServerboundInteractPacket.ActionType::class.java)
                if (type == ServerboundInteractPacket.ActionType.ATTACK) {
                    if (player.onGround() && !player.isInLava && !player.isInWater) {
                        if (legit) {
                            player.jumpFromGround()
                        } else {
                            netHandler.send(
                                ServerboundMovePlayerPacket.Pos(
                                    player.x,
                                    player.y + 0.03125,
                                    player.z,
                                    false,
                                    player.horizontalCollision
                                )
                            )
                            netHandler.send(
                                ServerboundMovePlayerPacket.Pos(
                                    player.x,
                                    player.y + 0.0625,
                                    player.z,
                                    false,
                                    player.horizontalCollision
                                )
                            )
                            netHandler.send(
                                ServerboundMovePlayerPacket.Pos(
                                    player.x,
                                    player.y,
                                    player.z,
                                    false,
                                    player.horizontalCollision
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}