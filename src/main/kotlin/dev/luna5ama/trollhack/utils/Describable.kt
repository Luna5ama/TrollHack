package dev.luna5ama.trollhack.utils

interface Describable {
    val description: CharSequence

    val descString
        get() = description.toString()
}