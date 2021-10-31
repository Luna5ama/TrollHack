package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.ConnectionEvent
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.accessor.x
import cum.xiaro.trollhack.util.accessor.y
import cum.xiaro.trollhack.util.accessor.z
import cum.xiaro.trollhack.util.atTrue
import cum.xiaro.trollhack.util.threads.runSafe
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.network.play.client.CPacketPlayer
import java.util.*

internal object Blink : Module(
    name = "Blink",
    category = Category.PLAYER,
    description = "Cancels server side packets"
) {
    private val cancelPacket by setting("Cancel Packets", false)
    private val autoReset0 = setting("Auto Reset", true)
    private val autoReset by autoReset0
    private val resetThreshold by setting("Reset Threshold", 20, 1..100, 5, autoReset0.atTrue())

    private const val ENTITY_ID = -114514
    private val packets = ArrayDeque<CPacketPlayer>()
    private var clonedPlayer: EntityOtherPlayerMP? = null
    private var sending = false

    init {
        onEnable {
            runSafe {
                begin()
            }
        }

        onDisable {
            end()
        }

        listener<PacketEvent.Send> {
            if (!sending && it.packet is CPacketPlayer) {
                it.cancel()
                packets.add(it.packet)
            }
        }

        safeListener<TickEvent.Post> {
            if (autoReset && packets.size >= resetThreshold) {
                end()
                begin()
            }
        }

        listener<ConnectionEvent.Disconnect> {
            mc.addScheduledTask {
                packets.clear()
                clonedPlayer = null
            }
        }
    }

    private fun SafeClientEvent.begin() {
        clonedPlayer = EntityOtherPlayerMP(mc.world, mc.session.profile).apply {
            copyLocationAndAnglesFrom(mc.player)
            rotationYawHead = mc.player.rotationYawHead
            inventory.copyInventory(mc.player.inventory)
            noClip = true
        }.also {
            mc.world.addEntityToWorld(ENTITY_ID, it)
        }
    }

    private fun end() {
        mc.addScheduledTask {
            runSafe {
                if (cancelPacket) {
                    packets.peek()?.let { player.setPosition(it.x, it.y, it.z) }
                    packets.clear()
                } else {
                    sending = true
                    while (packets.isNotEmpty()) connection.sendPacket(packets.poll())
                    sending = false
                }

                clonedPlayer?.setDead()
                world.removeEntityFromWorld(ENTITY_ID)
                clonedPlayer = null
            }

            packets.clear()
        }
    }

    override fun getHudInfo(): String {
        return packets.size.toString()
    }
}