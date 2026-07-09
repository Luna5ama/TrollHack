package dev.luna5ama.trollhack.event.impl.world

import dev.luna5ama.trollhack.event.api.EventBus
import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.event.api.IPosting
import net.minecraft.network.chat.Component

sealed class ConnectionEvent : IEvent {
    object Connect : ConnectionEvent(), IPosting by EventBus()
    class Disconnect(val message: Component) : ConnectionEvent(), IPosting by Companion {
        companion object : EventBus()
    }
}