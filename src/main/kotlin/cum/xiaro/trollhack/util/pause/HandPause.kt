package cum.xiaro.trollhack.util.pause

import net.minecraft.util.EnumHand

object HandPause {
    operator fun get(hand: EnumHand): PriorityTimeoutPause {
        return when (hand) {
            EnumHand.MAIN_HAND -> MainHandPause
            EnumHand.OFF_HAND -> OffhandPause
        }
    }
}


object MainHandPause : PriorityTimeoutPause()

object OffhandPause : PriorityTimeoutPause()