package me.luna.trollhack.event.events

import me.luna.trollhack.event.Event
import me.luna.trollhack.event.EventBus
import me.luna.trollhack.event.EventPosting
import net.minecraft.util.math.BlockPos

class BlockBreakEvent(val breakerID: Int, val position: BlockPos, val progress: Int) : Event, EventPosting by Companion {
    companion object : EventBus()
}