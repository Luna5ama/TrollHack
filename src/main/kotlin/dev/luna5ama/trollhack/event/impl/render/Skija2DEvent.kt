package dev.luna5ama.trollhack.event.impl.render

import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.event.api.IPosting
import dev.luna5ama.trollhack.event.api.EventBus
import io.github.humbleui.skija.Canvas
import io.github.humbleui.skija.DirectContext
import io.github.humbleui.skija.Surface

class Skija2DEvent(
    val context: DirectContext,
    val surface: Surface,
    val canvas: Canvas,
    val framebufferWidth: Int,
    val framebufferHeight: Int,
    val scaledWidth: Float,
    val scaledHeight: Float,
    val scale: Float,
    val ticksDelta: Float
) : IEvent, IPosting by Companion {
    companion object : EventBus() {
        val hasListeners: Boolean
            get() = handlers.isNotEmpty()
    }
}
