package dev.luna5ama.trollhack.utils

interface Nameable {
    val name: CharSequence
    val alias get() = setOf(name)
    val nameAsString get() = name.toString()

    val internalName: String
        get() = nameAsString.replace(" ", "")

    val allNames: Set<CharSequence>
        get() = setOf(name, internalName)
}