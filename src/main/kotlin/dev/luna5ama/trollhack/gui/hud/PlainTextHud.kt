package dev.luna5ama.trollhack.gui.hud

import dev.luna5ama.trollhack.gui.HudModule
import org.lwjgl.glfw.GLFW

abstract class PlainTextHud(
    name: CharSequence,
    description: CharSequence = "",
    hidden: Boolean = false,
    enableByDefault: Boolean = false,
    alwaysEnable: Boolean = false,
    defaultBind: Int = GLFW.GLFW_KEY_UNKNOWN,
    modulePriority: Int = 0,
    alias: Set<CharSequence> = setOf(name),
    internal: Boolean = false
) : HudModule(
    name,
    description,
    hidden,
    enableByDefault,
    alwaysEnable,
    defaultBind,
    modulePriority,
    alias,
    internal
) {
    abstract fun lines(): List<String>
}
