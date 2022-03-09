package me.luna.trollhack.event.events.render

import me.luna.trollhack.event.Event
import me.luna.trollhack.event.EventPosting
import me.luna.trollhack.event.NamedProfilerEventBus

object Render3DEvent : Event, EventPosting by NamedProfilerEventBus("trollRender3D")