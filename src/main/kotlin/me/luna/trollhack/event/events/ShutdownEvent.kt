package me.luna.trollhack.event.events

import me.luna.trollhack.event.Event
import me.luna.trollhack.event.EventBus
import me.luna.trollhack.event.EventPosting

object ShutdownEvent : Event, EventPosting by EventBus()