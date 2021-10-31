package cum.xiaro.trollhack.event.events

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventBus
import cum.xiaro.trollhack.event.EventPosting
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