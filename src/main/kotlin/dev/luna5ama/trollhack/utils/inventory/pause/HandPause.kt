package dev.luna5ama.trollhack.utils.inventory.pause

import net.minecraft.world.InteractionHand


object HandPause {
    operator fun get(hand: InteractionHand): PriorityTimeoutPause {
        return when (hand) {
            InteractionHand.MAIN_HAND -> MainHandPause
            InteractionHand.OFF_HAND -> OffhandPause
        }
    }
}


object MainHandPause : PriorityTimeoutPause()

object OffhandPause : PriorityTimeoutPause()