package dev.luna5ama.trollhack.event.api

import dev.luna5ama.trollhack.gui.hud.impl.HudEventPosts

interface IEvent : IPosting {
    fun post() {
        HudEventPosts.events.add(this::class.java.name)
        post(this)
    }
}