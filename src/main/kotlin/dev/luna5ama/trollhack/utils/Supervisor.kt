package dev.luna5ama.trollhack.utils

class Supervisor {
    var errorOccurred = false

    operator fun <R> invoke(op: Supervisor.() -> R): R = this.op()
}