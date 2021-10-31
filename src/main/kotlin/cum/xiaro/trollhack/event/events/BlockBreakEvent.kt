package cum.xiaro.trollhack.event.events

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventBus
import cum.xiaro.trollhack.event.EventPosting
import net.minecraft.util.math.BlockPos

class BlockBreakEvent(val breakerID: Int, val position: BlockPos, val progress: Int) : Event, EventPosting by Companion {
    companion object : EventBus()
}