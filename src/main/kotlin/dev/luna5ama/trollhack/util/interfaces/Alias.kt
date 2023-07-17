package dev.luna5ama.trollhack.util.interfaces

import dev.luna5ama.trollhack.util.extension.rootName

interface Alias : Nameable {
    val alias: Array<out CharSequence>

    override val allNames: Set<CharSequence>
        get() = mutableSetOf(name, internalName, rootName).apply {
            addAll(alias)
        }
}