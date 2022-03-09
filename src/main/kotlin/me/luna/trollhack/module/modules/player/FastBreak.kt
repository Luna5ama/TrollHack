package me.luna.trollhack.module.modules.player

import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.accessor.blockHitDelay
import me.luna.trollhack.util.threads.runSafe

internal object FastBreak : Module(
    name = "FastBreak",
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