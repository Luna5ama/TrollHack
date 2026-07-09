package dev.luna5ama.trollhack.event.impl.player

import dev.luna5ama.trollhack.event.api.*
import net.minecraft.client.player.ClientInput

class InputUpdateEvent(val movementInput: ClientInput) : IEvent, IPosting by Companion, ICancellable by Cancellable() {
    companion object : EventBus()
}