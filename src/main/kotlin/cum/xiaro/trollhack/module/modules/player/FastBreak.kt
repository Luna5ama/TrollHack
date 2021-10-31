package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.accessor.blockHitDelay
import cum.xiaro.trollhack.util.threads.runSafe

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