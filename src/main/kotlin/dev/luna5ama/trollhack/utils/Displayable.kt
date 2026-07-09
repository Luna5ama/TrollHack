package dev.luna5ama.trollhack.utils

interface Displayable {
    val displayName: CharSequence
        get() = this.toString()

    val displayString: String
        get() = displayName.toString()
}