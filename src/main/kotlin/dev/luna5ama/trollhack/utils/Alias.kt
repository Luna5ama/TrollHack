package dev.luna5ama.trollhack.utils

interface Alias : Nameable {
    override val alias: Set<CharSequence>

    override val allNames: Set<CharSequence>
        get() = mutableSetOf(name).apply {
            addAll(alias)
        }
}