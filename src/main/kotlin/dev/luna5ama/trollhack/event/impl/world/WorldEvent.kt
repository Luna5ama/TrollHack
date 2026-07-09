package dev.luna5ama.trollhack.event.impl.world

import dev.luna5ama.trollhack.event.api.EventBus
import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.event.api.IPosting
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity as McEntity

sealed class WorldEvent : IEvent {
    internal object Unload : WorldEvent(), IPosting by EventBus()
    internal object Load : WorldEvent(), IPosting by EventBus()

    sealed class Entity(val entity: McEntity) : WorldEvent() {
        class Add(entity: McEntity) : Entity(entity), IPosting by Companion {
            companion object : EventBus()
        }

        class Remove(entity: McEntity) : Entity(entity), IPosting by Companion {
            companion object : EventBus()
        }
    }

    data class ServerBlockUpdate(
        val pos: BlockPos,
        val oldState: BlockState,
        val newState: BlockState
    ) : WorldEvent(), IPosting by Companion {
        companion object : EventBus()
    }

    data class ClientBlockUpdate(
        val pos: BlockPos,
        val oldState: BlockState,
        val newState: BlockState
    ) : WorldEvent(), IPosting by Companion {
        companion object : EventBus()
    }

    data class RenderUpdate(
        val x1: Int,
        val y1: Int,
        val z1: Int,
        val x2: Int,
        val y2: Int,
        val z2: Int
    ) : WorldEvent(), IPosting by Companion {
        companion object : EventBus()
    }
}