package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.manager.managers.TimerManager.modifyTimer
import dev.luna5ama.trollhack.manager.managers.TimerManager.resetTimer
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.MovementUtils
import dev.luna5ama.trollhack.util.atFalse
import dev.luna5ama.trollhack.util.atTrue

internal object Timer : Module(
    name = "Timer",
    category = Category.PLAYER,
    description = "Changes your client tick speed",
    modulePriority = 500
) {
    private val pauseOnMove by setting("Pause On Move", false)
    private val pauseOnSteady by setting("Pause On Steady", false)
    private val slow0 = setting("Slow Mode", false)
    private val slow by slow0
    private val tickNormal by setting("Tick N", 2.0f, 1f..10f, 0.1f, slow0.atFalse())
    private val tickSlow by setting("Tick S", 8f, 1f..10f, 0.1f, slow0.atTrue())

    init {
        onDisable {
            resetTimer()
        }

        listener<RunGameLoopEvent.Start> {
            val inputting = MovementUtils.isInputting(jump = true)
            if (pauseOnMove && inputting || pauseOnSteady && !inputting) {
                resetTimer()
                return@listener
            }

            val multiplier = if (!slow) tickNormal else tickSlow / 10.0f
            modifyTimer(50.0f / multiplier)
        }
    }
}