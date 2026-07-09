package dev.luna5ama.trollhack.utils.input

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.*

data class KeyBind @JvmOverloads constructor(
    var category: Category = Category.KEYBOARD,
    var keyCode: Int = GLFW_KEY_UNKNOWN,
    var scanCode: Int = if (keyCode == GLFW_KEY_UNKNOWN) -1 else glfwGetKeyScancode(keyCode)
) {
    val keyName: String
        get() = getKeyName(keyCode, scanCode)

    companion object {
        private val fields = org.lwjgl.glfw.GLFW::class.java.fields
        val NONE = KeyBind()

        fun getKeyName(key: Int, scanCode: Int): String {
            return when (key) {
                GLFW_KEY_UNKNOWN -> "Unknown"
                GLFW_KEY_ESCAPE -> "Esc"
                GLFW_KEY_GRAVE_ACCENT -> "Grave"
                GLFW_KEY_WORLD_1 -> "World 1"
                GLFW_KEY_WORLD_2 -> "World 2"
                GLFW_KEY_PRINT_SCREEN -> "Print Screen"
                GLFW_KEY_PAUSE -> "Pause"
                GLFW_KEY_INSERT -> "Insert"
                GLFW_KEY_DELETE -> "Delete"
                GLFW_KEY_HOME -> "Home"
                GLFW_KEY_PAGE_UP -> "Page Up"
                GLFW_KEY_PAGE_DOWN -> "Page Down"
                GLFW_KEY_END -> "End"
                GLFW_KEY_TAB -> "Tab"
                GLFW_KEY_LEFT_CONTROL -> "LControl"
                GLFW_KEY_RIGHT_CONTROL -> "RControl"
                GLFW_KEY_LEFT_ALT -> "LAlt"
                GLFW_KEY_RIGHT_ALT -> "RAlt"
                GLFW_KEY_LEFT_SHIFT -> "LShift"
                GLFW_KEY_RIGHT_SHIFT -> "RShift"
                GLFW_KEY_UP -> "Up"
                GLFW_KEY_DOWN -> "Down"
                GLFW_KEY_LEFT -> "Left"
                GLFW_KEY_RIGHT -> "Right"
                GLFW_KEY_APOSTROPHE -> "Apostrophe"
                GLFW_KEY_BACKSPACE -> "Backspace"
                GLFW_KEY_CAPS_LOCK -> "Caps Lock"
                GLFW_KEY_MENU -> "Menu"
                GLFW_KEY_LEFT_SUPER -> "LSuper"
                GLFW_KEY_RIGHT_SUPER -> "RSuper"
                GLFW_KEY_ENTER -> "Enter"
                GLFW_KEY_KP_ENTER -> "Numpad Enter"
                GLFW_KEY_NUM_LOCK -> "NumLock"
                GLFW_KEY_SPACE -> "Space"
                GLFW_KEY_F1 -> "F1"
                GLFW_KEY_F2 -> "F2"
                GLFW_KEY_F3 -> "F3"
                GLFW_KEY_F4 -> "F4"
                GLFW_KEY_F5 -> "F5"
                GLFW_KEY_F6 -> "F6"
                GLFW_KEY_F7 -> "F7"
                GLFW_KEY_F8 -> "F8"
                GLFW_KEY_F9 -> "F9"
                GLFW_KEY_F10 -> "F10"
                GLFW_KEY_F11 -> "F11"
                GLFW_KEY_F12 -> "F12"
                GLFW_KEY_F13 -> "F13"
                GLFW_KEY_F14 -> "F14"
                GLFW_KEY_F15 -> "F15"
                GLFW_KEY_F16 -> "F16"
                GLFW_KEY_F17 -> "F17"
                GLFW_KEY_F18 -> "F18"
                GLFW_KEY_F19 -> "F19"
                GLFW_KEY_F20 -> "F20"
                GLFW_KEY_F21 -> "F21"
                GLFW_KEY_F22 -> "F22"
                GLFW_KEY_F23 -> "F23"
                GLFW_KEY_F24 -> "F24"
                GLFW_KEY_F25 -> "F25"
                else -> {
                    glfwGetKeyName(key, scanCode) ?: "Unknown"
                }
            }
        }

        fun fromRaw(name: String): KeyBind {
            val prefix =
                if (name.startsWith("MOUSE")) "MOUSE"
                else if (name.startsWith(("GAMEPAD"))) "GAMEPAD"
                else "KEY"
            val raw = name.removePrefix("MOUSE_").removePrefix("GAMEPAD_").removePrefix("KEY_")
            val key = (fields.find { it.name.equals("GLFW_${prefix}_$raw", true) }
                ?.get(null) as? Int)
                ?: throw IllegalArgumentException(
                    "Unknown key $name. Check your input or try to specific a category among MOUSE, GAMEPAD and KEYBOARD"
                )
            return KeyBind(Category.from(prefix), key, glfwGetKeyScancode(key))
        }

        fun fromClassified(string: String): KeyBind {
            val split = string.split(":")
            val category = Category.valueOf(split[0])
            val keyCode = split[1].toInt()
            val scanCode = split[2].toInt()
            return KeyBind(category, keyCode, scanCode)
        }
    }

    override fun toString(): String = "${category.name}:$keyCode:$scanCode"

    enum class Category {
        KEYBOARD,
        MOUSE,
        GAMEPAD;

        companion object {
            fun from(string: String) = when (string) {
                "KEY" -> KEYBOARD
                "MOUSE" -> MOUSE
                "GAMEPAD" -> GAMEPAD
                else -> throw IllegalArgumentException()
            }
        }
    }
}