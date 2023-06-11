package dev.luna5ama.trollhack.event.events

import dev.luna5ama.trollhack.event.*
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import net.minecraft.network.Packet

sealed class PacketEvent(val packet: Packet<*>) : Event {
    abstract val side: Side
    abstract val stage: Stage

    class Receive(packet: Packet<*>) : PacketEvent(packet), ICancellable by Cancellable(), EventPosting by Companion {
        override val side: Side
            get() = Side.SERVER

        override val stage: Stage
            get() = Stage.PRE

        companion object : EventBus()
    }

    class PostReceive(packet: Packet<*>) : PacketEvent(packet), EventPosting by Companion {
        override val side: Side
            get() = Side.SERVER

        override val stage: Stage
            get() = Stage.POST

        companion object : EventBus()
    }

    class Send(packet: Packet<*>) : PacketEvent(packet), ICancellable by Cancellable(), EventPosting by Companion {
        override val side: Side
            get() = Side.CLIENT

        override val stage: Stage
            get() = Stage.PRE

        companion object : EventBus()
    }

    class PostSend(packet: Packet<*>) : PacketEvent(packet), EventPosting by Companion {
        override val side: Side
            get() = Side.CLIENT

        override val stage: Stage
            get() = Stage.POST

        companion object : EventBus()
    }

    enum class Side(override val displayName: CharSequence) : DisplayEnum {
        CLIENT("Client"),
        SERVER("Server");

        override fun toString(): String {
            return displayString
        }
    }

    enum class Stage(override val displayName: CharSequence) : DisplayEnum {
        PRE("Pre"),
        POST("Post");

        override fun toString(): String {
            return displayString
        }
    }
}