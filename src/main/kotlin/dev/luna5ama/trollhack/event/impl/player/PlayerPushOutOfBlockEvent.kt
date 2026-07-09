package dev.luna5ama.trollhack.event.impl.player

import dev.luna5ama.trollhack.event.api.*

sealed class PlayerPushOutOfBlockEvent : IEvent {
    class Push : PlayerPushOutOfBlockEvent(), ICancellable by Cancellable(), IPosting by Push {
        companion object : EventBus()
    }
}