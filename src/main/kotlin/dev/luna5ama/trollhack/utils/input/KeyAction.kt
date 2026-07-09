package dev.luna5ama.trollhack.utils.input

import org.lwjgl.glfw.GLFW


enum class KeyAction {
    Press,
    Repeat,
    Release;

    companion object {
        operator fun get(action: Int): KeyAction {
            return if (action == GLFW.GLFW_PRESS) Press else if (action == GLFW.GLFW_RELEASE) Release else Repeat
        }
    }
}