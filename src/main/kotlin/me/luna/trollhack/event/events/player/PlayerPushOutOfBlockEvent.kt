package me.luna.trollhack.event.events.player

import me.luna.trollhack.event.*
import net.minecraftforge.client.event.PlayerSPPushOutOfBlocksEvent

class PlayerPushOutOfBlockEvent(override val event: PlayerSPPushOutOfBlocksEvent) : Event, ICancellable, WrappedForgeEvent, EventPosting by Companion {
    companion object : EventBus()
}