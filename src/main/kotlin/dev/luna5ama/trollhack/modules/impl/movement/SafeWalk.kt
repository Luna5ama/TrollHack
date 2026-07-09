package dev.luna5ama.trollhack.modules.impl.movement

import dev.luna5ama.trollhack.manager.managers.EntityMovementManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module

object SafeWalk: Module("Safe Walk","a safe walk",Category.MOVEMENT) {
    val eagle by setting("Eagle", false)

    init {
        onEnabled {
            if (!eagle) EntityMovementManager.isSafeWalk = true
        }

        onDisabled {
            if (!eagle) EntityMovementManager.isSafeWalk = false
        }
    }
}