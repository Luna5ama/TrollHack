package dev.luna5ama.trollhack.gui

import dev.luna5ama.trollhack.RS
import dev.luna5ama.trollhack.modules.AbstractModule
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.utils.MinecraftWrapper
import org.lwjgl.glfw.GLFW

@Suppress("LeakingThis")
abstract class HudModule(
    name: CharSequence,
    description: CharSequence = "",
    val hudCategory: HudCategory = HudCategory.CLIENT,
    hidden: Boolean = false,
    enableByDefault: Boolean = false,
    alwaysEnable: Boolean = false,
    defaultBind: Int = GLFW.GLFW_KEY_UNKNOWN,
    modulePriority: Int = 0,
    alias: Set<CharSequence> = setOf(name),
    internal: Boolean = false
) : AbstractModule(name, description, Category.HUD, hidden, false,
    enableByDefault, alwaysEnable, defaultBind, modulePriority, alias, internal) {
    private var _x_normalized by setting("__internal__X__", 0.1f, 0.0f..1.0f, visibility = { false })
    private var _y_normalized by setting("__internal__Y__", 0.05f, 0.0f..1.0f, visibility = { false })
    var _x
        set(value) {
            _x_normalized = value / RS.scaledWidthF
        }
        get() = _x_normalized * RS.scaledWidthF

    var _y
        set(value) {
            _y_normalized = value / RS.scaledHeightF
        }
        get() = _y_normalized * RS.scaledHeightF
}

enum class HudCategory(val displayName: String) {
    CLIENT("Client"),
    COMBAT("Combat"),
    PLAYER("Player"),
    WORLD("World"),
    MISC("Misc")
}
