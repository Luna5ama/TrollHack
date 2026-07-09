package dev.luna5ama.trollhack.gui.hud.impl

import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.impl.LoopEvent
import dev.luna5ama.trollhack.gui.hud.PlainTextHud
import dev.luna5ama.trollhack.graphics.font.TextComponent
import java.util.concurrent.CopyOnWriteArraySet

object HudEventPosts : PlainTextHud("Event Posts") {
    val events = CopyOnWriteArraySet<String>()

    private var eventsCache = emptyList<String>()

    init {
        handler<LoopEvent.Start> {
            events.clear()
        }

        handler<LoopEvent.End> {
            eventsCache = events.toList()
        }
    }

    context(TextInfo)
    override fun TextComponent.buildText() {
        eventsCache.forEach {
            addLine(it)
        }
    }
}