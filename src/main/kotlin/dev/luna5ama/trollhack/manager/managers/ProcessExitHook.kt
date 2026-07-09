package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.utils.NamedTask
import dev.luna5ama.trollhack.utils.Task

object ProcessExitHook {
    private val tasks: MutableList<NamedTask<Unit>> = ArrayList()

    fun register(task: NamedTask<Unit>) {
        tasks.add(task)
    }

    fun register(task: Task<Unit>) {
        tasks.add(task.toNamedTask())
    }

    fun onExit() {
        tasks.forEach(NamedTask<Unit>::run)
        tasks.clear()
    }
}