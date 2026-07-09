package dev.luna5ama.trollhack.utils

import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod

fun interface Task<R> {
    fun run(): R

    fun toNamedTask(name: CharSequence = "${TrollHackMod.NAME}-DefaultTaskName"): NamedTask<R> {
        return NamedTask { this@Task.run() }
    }
}

fun interface NamedTask<R> : Task<R>, Nameable {
    override val name: CharSequence
        get() = "${TrollHackMod.NAME}-DefaultTaskName"
}