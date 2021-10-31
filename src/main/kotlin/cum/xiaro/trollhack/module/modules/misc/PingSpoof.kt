package cum.xiaro.trollhack.module.modules.misc

import cum.xiaro.trollhack.util.interfaces.DisplayEnum
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.threads.defaultScope
import cum.xiaro.trollhack.util.threads.onMainThreadSafeSuspend
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.network.play.client.CPacketConfirmTransaction
import net.minecraft.network.play.client.CPacketKeepAlive
import net.minecraft.network.play.server.SPacketConfirmTransaction
import net.minecraft.network.play.server.SPacketKeepAlive

internal object PingSpoof : Module(
    name = "PingSpoof",
    category = Category.MISC,
    description = "Cancels or adds delay to your ping packets"
) {
    private val mode by setting("Mode", Mode.NORMAL)
    private val delay by setting("Delay", 100, 0..1000, 5)
    private val multiplier by setting("Multiplier", 1, 1..100, 1)

    private enum class Mode(override val displayName: CharSequence) : DisplayEnum {
        NORMAL("Normal"),
        CC("CC")
    }

    private val packetTimer = TickTimer()

    override fun getHudInfo(): String {
        return (delay * multiplier).toString()
    }

    init {
        onDisable {
            packetTimer.reset(-114514L)
        }

        listener<PacketEvent.Receive> {
            when (it.packet) {
                is SPacketKeepAlive -> {
                    packetTimer.reset()
                    it.cancel()
                    defaultScope.launch {
                        delay((delay * multiplier).toLong())
                        onMainThreadSafeSuspend {
                            connection.sendPacket(CPacketKeepAlive(it.packet.id))
                        }
                    }
                }
                is SPacketConfirmTransaction -> {
                    if (mode == Mode.CC && it.packet.windowId == 0 && !it.packet.wasAccepted() && !packetTimer.tickAndReset(1L)) {
                        packetTimer.reset(-114514L)
                        it.cancel()
                        defaultScope.launch {
                            delay((delay * multiplier).toLong())
                            onMainThreadSafeSuspend {
                                connection.sendPacket(CPacketConfirmTransaction(it.packet.windowId, it.packet.actionNumber, true))
                            }
                        }
                    }
                }
            }
        }
    }
}
