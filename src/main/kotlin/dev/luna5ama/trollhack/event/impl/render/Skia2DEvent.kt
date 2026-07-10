package dev.luna5ama.trollhack.event.impl.render

import dev.luna5ama.trollhack.event.api.EventBus
import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.event.api.IPosting
import dev.luna5ama.trollhack.graphics.skia.SkiaDrawScope
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.Surface

class Skia2DEvent(
    val context: DirectContext,
    val surface: Surface,
    val canvas: Canvas,
    val framebufferWidth: Int,
    val framebufferHeight: Int,
    val width: Float,
    val height: Float,
    val scale: Float,
    val ticksDelta: Float
) : IEvent, IPosting by Companion {
    val draw = SkiaDrawScope(canvas)

    companion object : EventBus() {
        val hasListeners: Boolean
            get() = handlers.isNotEmpty()
    }
}
