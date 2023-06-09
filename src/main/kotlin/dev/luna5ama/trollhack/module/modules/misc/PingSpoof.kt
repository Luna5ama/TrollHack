package dev.luna5ama.trollhack.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.threads.DefaultScope
import dev.luna5ama.trollhack.util.threads.onMainThreadSafe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.network.play.client.CPacketConfirmTransaction
import net.minecraft.network.play.client.CPacketKeepAlive
import net.minecraft.network.play.server.SPacketConfirmTransaction
import net.minecraft.network.play.server.SPacketKeepAlive

internal object PingSpoof : Module(
    name = "Ping Spoof",
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
                    DefaultScope.launch {
                        delay((delay * multiplier).toLong())
                        onMainThreadSafe {
                            connection.sendPacket(CPacketKeepAlive(it.packet.id))
                        }
                    }
                }
                is SPacketConfirmTransaction -> {
                    if (mode == Mode.CC && it.packet.windowId == 0 && !it.packet.wasAccepted() && !packetTimer.tickAndReset(
                            1L
                        )
                    ) {
                        packetTimer.reset(-114514L)
                        it.cancel()
                        DefaultScope.launch {
                            delay((delay * multiplier).toLong())
                            onMainThreadSafe {
                                connection.sendPacket(
                                    CPacketConfirmTransaction(
                                        it.packet.windowId,
                                        it.packet.actionNumber,
                                        true
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}