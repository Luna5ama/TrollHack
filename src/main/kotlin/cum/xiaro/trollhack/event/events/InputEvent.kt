package cum.xiaro.trollhack.event.events

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventBus
import cum.xiaro.trollhack.event.EventPosting

sealed class InputEvent(val state: Boolean) : Event {
    class Keyboard(val key: Int, state: Boolean) : InputEvent(state), EventPosting by Companion {
        companion object : EventBus()
    }

    class Mouse(val button: Int, state: Boolean) : InputEvent(state), EventPosting by Companion {
        companion object : EventBus()
    }
}
