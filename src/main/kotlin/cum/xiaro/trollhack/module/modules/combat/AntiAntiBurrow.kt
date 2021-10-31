package cum.xiaro.trollhack.module.modules.combat

import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import cum.xiaro.trollhack.event.events.player.PlayerMoveEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.event.safeParallelListener
import cum.xiaro.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.EntityUtils.flooredPosition
import cum.xiaro.trollhack.util.world.getCollisionBox
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.server.SPacketCloseWindow
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.max

internal object AntiAntiBurrow : Module(
    name = "AntiAntiBurrow",
    category = Category.COMBAT,
    description = "Fuck you ab"
) {
    private var jumping = false
    private var prevBurrowed = false
    private var burrowed = false
    private var burrowPos = BlockPos.ORIGIN
    private var doneFlyUp = false

    init {
        onDisable {
            prevBurrowed = false
            burrowed = false
            burrowPos = BlockPos.ORIGIN
            jumping = false
            doneFlyUp = false
        }

        safeListener<PacketEvent.Receive> {
            if (burrowed) {
                when (it.packet) {
                    is SPacketCloseWindow -> {
                        it.cancel()
                    }
                    is SPacketPlayerPosLook -> {
                        if (jumping) {
                            var y = it.packet.y

                            if (it.packet.flags.contains(SPacketPlayerPosLook.EnumFlags.Y)) {
                                y += player.posY
                            }

                            if (y > burrowPos.y) {
                                doneFlyUp = true
                            }
                        }
                    }
                }
            }
        }

        safeParallelListener<TickEvent.Post> {
            val pos = player.flooredPosition

            jumping = mc.gameSettings.keyBindJump.isKeyDown
            burrowed = world.collidesWithBlock(pos)

            if (prevBurrowed != burrowed) {
                if (!prevBurrowed) {
                    burrowPos = pos
                }
                doneFlyUp = false
            }

            prevBurrowed = burrowed
        }

        safeListener<PlayerMoveEvent.Pre> {
            if (burrowed && !doneFlyUp) {
                player.motionY = max(player.motionY, 0.0)
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (burrowed && !doneFlyUp) {
                sendPlayerPacket {
                    if (jumping) {
                        if (player.posY - burrowPos.y < 0.1) {
                            move(Vec3d(player.posX, player.posY + 0.0622, player.posZ))
                            onGround(false)
                        }
                    } else if (player.posY >= burrowPos.y) {
                        move(Vec3d(player.posX, player.posY - 0.0622, player.posZ))
                        onGround(false)
                    } else {
                        player.collidedVertically = true
                        player.onGround = true
                        onGround(true)
                    }
                }
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Post> {
            if (burrowed && !doneFlyUp) {
                connection.sendPacket(CPacketPlayer.Position(player.posX, player.posY - 69420.0, player.posZ, true))
            }
        }
    }

    private fun WorldClient.collidesWithBlock(pos: BlockPos): Boolean {
        return this.getCollisionBox(pos) != null
    }
}