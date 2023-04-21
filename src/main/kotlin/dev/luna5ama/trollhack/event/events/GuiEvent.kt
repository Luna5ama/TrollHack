package dev.luna5ama.trollhack.event.events

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import net.minecraft.client.gui.GuiScreen

sealed class GuiEvent : Event {
    abstract val screen: GuiScreen?

    class Closed(override val screen: GuiScreen) : GuiEvent(), EventPosting by Companion {
        companion object : EventBus()
    }

    class Displayed(override var screen: GuiScreen?) : GuiEvent(), EventPosting by Companion {
        companion object : EventBus()
    }
}