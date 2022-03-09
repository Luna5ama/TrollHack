package me.luna.trollhack.event.events

import me.luna.trollhack.event.Event
import me.luna.trollhack.event.EventBus
import me.luna.trollhack.event.EventPosting

sealed class InputEvent(val state: Boolean) : Event {
    class Keyboard(val key: Int, state: Boolean) : InputEvent(state), EventPosting by Companion {
        companion object : EventBus()
    }

    class Mouse(val button: Int, state: Boolean) : InputEvent(state), EventPosting by Companion {
        companion object : EventBus()
    }
}
