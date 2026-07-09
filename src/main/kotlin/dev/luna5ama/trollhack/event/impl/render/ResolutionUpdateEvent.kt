package dev.luna5ama.trollhack.event.impl.render

import dev.luna5ama.trollhack.event.api.EventBus
import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.event.api.IPosting

class ResolutionUpdateEvent(val framebufferWidth: Int, val framebufferHeight: Int) : IEvent, IPosting by Companion {
    companion object : EventBus()
}