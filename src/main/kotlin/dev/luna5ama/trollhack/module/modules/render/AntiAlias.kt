package dev.luna5ama.trollhack.module.modules.render

import dev.luna5ama.trollhack.event.events.render.ResolutionUpdateEvent
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.threads.onMainThread

internal object AntiAlias : Module(
    name = "AntiAlias",
    description = "Enables Antialias",
    category = Category.RENDER
) {
    private val sampleLevel0 = setting("SSAA Level", 1.0f, 1.0f..2.0f, 0.05f)

    val sampleLevel get() = if (isEnabled) sampleLevel0.value else 1.0f

    init {
        onToggle {
            onMainThread {
                mc.resize(mc.displayWidth, mc.displayHeight)
                ResolutionUpdateEvent(mc.displayWidth, mc.displayHeight).post()
            }
        }

        sampleLevel0.listeners.add {
            onMainThread {
                mc.resize(mc.displayWidth, mc.displayHeight)
                ResolutionUpdateEvent(mc.displayWidth, mc.displayHeight).post()
            }
        }
    }
}
