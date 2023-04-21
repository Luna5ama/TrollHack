package dev.luna5ama.trollhack.util.interfaces

interface Nameable {
    val name: CharSequence
    val nameAsString: String
        get() = name.toString()
}
