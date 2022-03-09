package me.luna.trollhack.module.modules.player

import me.luna.trollhack.event.events.RunGameLoopEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.manager.managers.TimerManager.modifyTimer
import me.luna.trollhack.manager.managers.TimerManager.resetTimer
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.atFalse
import me.luna.trollhack.util.atTrue

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