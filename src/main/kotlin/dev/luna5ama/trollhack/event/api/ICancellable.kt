package dev.luna5ama.trollhack.event.api

interface ICancellable {
    var cancelled: Boolean

    fun cancel() {
        cancelled = true
    }
}

open class Cancellable : ICancellable {
    override var cancelled = false
        set(value) {
            field = field || value
        }
}