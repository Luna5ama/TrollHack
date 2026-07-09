package dev.luna5ama.trollhack.event.impl.player

import dev.luna5ama.trollhack.event.api.EventBus
import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.event.api.IPosting

sealed interface PlayerJumpEvent : IEvent, IPosting {
    data object Pre : EventBus(), PlayerJumpEvent
    data object Post : EventBus(), PlayerJumpEvent
}