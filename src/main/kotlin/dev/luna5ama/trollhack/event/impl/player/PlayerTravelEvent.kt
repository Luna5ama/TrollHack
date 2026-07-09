package dev.luna5ama.trollhack.event.impl.player

import dev.luna5ama.trollhack.event.api.*
import net.minecraft.world.entity.Entity

sealed class PlayerTravelEvent(var entity: Entity) : IEvent {
    class Pre(entity: Entity) : PlayerTravelEvent(entity), ICancellable by Cancellable(), IPosting by Companion {
        companion object : EventBus()
    }

    class Post(entity: Entity) : PlayerTravelEvent(entity), ICancellable by Cancellable(), IPosting by Companion {
        companion object : EventBus()
    }
}