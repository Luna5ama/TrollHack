package dev.luna5ama.trollhack.event.events

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import dev.luna5ama.trollhack.module.AbstractModule

class ModuleToggleEvent internal constructor(val module: AbstractModule) : Event, EventPosting by Companion {
    companion object : EventBus()
}