package dev.luna5ama.trollhack.event.impl.world

import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.event.api.EventBus
import dev.luna5ama.trollhack.event.api.IPosting
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block

class PlaceBlockEvent(val pos: BlockPos, val block: Block) :IEvent, IPosting by PlaceBlockEvent {
    companion object : EventBus()

    fun getBlockPos(): BlockPos {
        return pos
    }

    fun getBlockState(): Block {
        return block
    }
}