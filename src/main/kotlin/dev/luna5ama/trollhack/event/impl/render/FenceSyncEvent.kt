package dev.luna5ama.trollhack.event.impl.render

import dev.luna5ama.trollhack.event.api.EventBus
import dev.luna5ama.trollhack.event.api.IEvent
import dev.luna5ama.trollhack.event.api.IPosting

object FenceSyncEvent : IEvent, IPosting by EventBus()