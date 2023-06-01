package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.accessor.blockHitDelay
import dev.luna5ama.trollhack.util.threads.runSafe

internal object FastBreak : Module(
    name = "Fast Break",
    category = Category.PLAYER,
    description = "Breaks block faster and nullifies the break delay"
) {
    private val breakDelay by setting("Break Delay", 0, 0..5, 1)

    @JvmStatic
    fun updateBreakDelay() {
        runSafe {
            if (isEnabled) {
                playerController.blockHitDelay = breakDelay
            }
        }
    }
}