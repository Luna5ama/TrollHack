package me.luna.trollhack.event.events

import me.luna.trollhack.event.Event
import me.luna.trollhack.event.EventBus
import me.luna.trollhack.event.EventPosting
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