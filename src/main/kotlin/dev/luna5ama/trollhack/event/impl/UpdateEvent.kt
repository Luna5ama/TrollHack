package dev.luna5ama.trollhack.event.impl

import dev.luna5ama.trollhack.event.api.EventBus
import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.event.api.IPosting

object UpdateEvent : IEvent, IPosting by EventBus()