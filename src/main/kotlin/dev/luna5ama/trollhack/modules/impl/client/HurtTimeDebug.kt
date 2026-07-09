package dev.luna5ama.trollhack.modules.impl.client

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.LoopEvent
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.ChatUtils
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket

object HurtTimeDebug: Module("Hurttime Debug","hurttime debug",Category.CLIENT) {

    private val velocityCheck by setting("Velocity Check",false)

    private var loopCount = 0
    private var lastHurttime = 0
    private var velocity = false

    override fun getDisplayInfo(): Any {
        return "$loopCount|$lastHurttime,$velocity|"
    }

    init {

        onDisabled {
            loopCount = 0
            lastHurttime = 0
            velocity = false
        }

        nonNullHandler<LoopEvent.Tick> {

            if ((player.hurtTime != 0) && lastHurttime == player.hurtTime){
                loopCount++
                return@nonNullHandler
            }
            lastHurttime = player.hurtTime

            if (velocity && player.hurtTime != 0){
                velocity = false
                ChatUtils.sendMessage("EntityVelocityUpdateS2CPacket was detected at hurttime ${player.hurtTime}, and it looped $loopCount")
                loopCount = 0
            }

        }

        nonNullHandler<PacketEvent.Receive> {
            if (it.packet is ClientboundSetEntityMotionPacket && it.packet.id == player.id && velocityCheck){
                velocity = true
            }
        }
    }

}