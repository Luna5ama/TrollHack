package dev.luna5ama.trollhack.event.impl.render

import com.mojang.blaze3d.vertex.PoseStack
import dev.luna5ama.trollhack.event.api.EventBus
import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.event.api.IPosting

class Render3DEvent(val matrixStack: PoseStack, val partialTicks: Float) : IEvent, IPosting by Companion {
    companion object : EventBus()
}