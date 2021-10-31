package cum.xiaro.trollhack.event.events

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventBus
import cum.xiaro.trollhack.event.EventPosting
import cum.xiaro.trollhack.module.AbstractModule

class ModuleToggleEvent internal constructor(val module: AbstractModule) : Event, EventPosting by Companion {
    companion object : EventBus()
}