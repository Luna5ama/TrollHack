@file:Suppress("UNUSED")

package dev.luna5ama.trollhack.util

import dev.luna5ama.trollhack.event.SafeClientEvent
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.util.EnumHand

enum class SwingMode {
    CLIENT {
        override fun swingHand(event: SafeClientEvent, hand: EnumHand) {
            event.player.swingArm(hand)
        }
    },
    PACKET {
        override fun swingHand(event: SafeClientEvent, hand: EnumHand) {
            event.connection.sendPacket(CPacketAnimation(hand))
        }
    };

    abstract fun swingHand(event: SafeClientEvent, hand: EnumHand)
}
