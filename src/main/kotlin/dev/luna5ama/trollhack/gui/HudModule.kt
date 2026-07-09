package dev.luna5ama.trollhack.gui

import dev.luna5ama.trollhack.RS
import dev.luna5ama.trollhack.modules.AbstractModule
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.utils.MinecraftWrapper
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.math.vectors.Vec2i
import org.lwjgl.glfw.GLFW

@Suppress("LeakingThis")
abstract class HudModule(
    name: CharSequence,
    description: CharSequence = "",
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
    protected abstract val width: Float
    protected abstract val height: Float
    private var dragging = false
    private var offset = Vec2f.ZERO

    fun onMouseMove(mousePos: Vec2i): Boolean {
        return if (dragging) {
            val (newX, newY) = mousePos - offset
            _x = newX
            _y = newY
            true
        } else false
    }

    fun onMouseClicked(mousePos: Vec2i): Boolean {
        return if (isHovered(mousePos)) {
            dragging = true
            offset = mousePos - Vec2f(_x, _y)
            true
        } else false
    }

    fun onMouseRelease(mousePos: Vec2i): Boolean {
        return if (isHovered(mousePos)) {
            if (dragging) dragging = false
            true
        } else false
    }

    abstract fun onRender2D(x: Float, y: Float)

    protected fun isHovered(mousePos: Vec2i): Boolean {
        return mousePos.x.toFloat() in _x..(_x + width)
                && mousePos.y.toFloat() in _y..(_y + height)
    }
}