package dev.luna5ama.trollhack.event.events

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting

sealed class InputEvent(val state: Boolean) : Event {
    class Keyboard(val key: Int, state: Boolean) : InputEvent(state), EventPosting by Companion {
        companion object : EventBus()
    }

    class Mouse(val button: Int, state: Boolean) : InputEvent(state), EventPosting by Companion {
        companion object : EventBus()
    }
}
