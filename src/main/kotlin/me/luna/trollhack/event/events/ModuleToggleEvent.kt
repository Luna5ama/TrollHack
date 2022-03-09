package me.luna.trollhack.event.events

import me.luna.trollhack.event.Event
import me.luna.trollhack.event.EventBus
import me.luna.trollhack.event.EventPosting
import me.luna.trollhack.module.AbstractModule

class ModuleToggleEvent internal constructor(val module: AbstractModule) : Event, EventPosting by Companion {
    companion object : EventBus()
}