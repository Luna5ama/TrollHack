package dev.luna5ama.trollhack.event.api


interface IPosting {
    val eventBus: EventBus

    fun post(event: Any)
}


