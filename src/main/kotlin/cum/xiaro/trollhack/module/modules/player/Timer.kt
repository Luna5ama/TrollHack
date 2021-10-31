package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.event.events.RunGameLoopEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.manager.managers.TimerManager.modifyTimer
import cum.xiaro.trollhack.manager.managers.TimerManager.resetTimer
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.atFalse
import cum.xiaro.trollhack.util.atTrue

internal object Timer : Module(
    name = "Timer",
    category = Category.PLAYER,
    description = "Changes your client tick speed",
    modulePriority = 500
) {
    private val slow0 = setting("Slow Mode", false)
    private val slow by slow0
    private val tickNormal by setting("Tick N", 2.0f, 1f..10f, 0.1f, slow0.atFalse())
    private val tickSlow by setting("Tick S", 8f, 1f..10f, 0.1f, slow0.atTrue())

    init {
        onDisable {
            resetTimer()
        }

        listener<RunGameLoopEvent.Start> {
            val multiplier = if (!slow) tickNormal else tickSlow / 10.0f
            modifyTimer(50.0f / multiplier)
        }
    }
}