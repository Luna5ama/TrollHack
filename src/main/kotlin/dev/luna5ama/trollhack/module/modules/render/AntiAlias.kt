package dev.luna5ama.trollhack.module.modules.render

import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.render.ResolutionUpdateEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.threads.onMainThread
import org.lwjgl.opengl.Display

internal object AntiAlias : Module(
    name = "Anti Alias",
    description = "Enables Antialias",
    category = Category.RENDER
) {
    private val disableInBackground by setting("Disable In Background", true)
    private val sampleLevel0 = setting("SSAA Level", 1.0f, 1.0f..2.0f, 0.05f)

    val sampleLevel get() = if (isEnabled && Display.isActive()) sampleLevel0.value else 1.0f
    private var prevSampleLevel = 1.0f

    init {
        onToggle {
            onMainThread {
                mc.resize(mc.displayWidth, mc.displayHeight)
                ResolutionUpdateEvent(mc.displayWidth, mc.displayHeight).post()
            }
        }

        listener<TickEvent.Pre> {
            val sampleLevel = sampleLevel
            if (sampleLevel != prevSampleLevel) {
                prevSampleLevel = sampleLevel
                mc.resize(mc.displayWidth, mc.displayHeight)
                ResolutionUpdateEvent(mc.displayWidth, mc.displayHeight).post()
            }
        }
    }
}