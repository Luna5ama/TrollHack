package dev.luna5ama.trollhack.event.events

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import net.minecraft.util.math.BlockPos

class BlockBreakEvent(val breakerID: Int, val position: BlockPos, val progress: Int) : Event,
    EventPosting by Companion {
    companion object : EventBus()
}