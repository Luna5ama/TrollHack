package dev.luna5ama.trollhack.gui.rgui

import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.setting.GuiConfig
import dev.luna5ama.trollhack.setting.configs.AbstractConfig
import dev.luna5ama.trollhack.util.interfaces.Nameable
import dev.luna5ama.trollhack.util.math.vector.Vec2f

open class InteractiveComponent(
    screen: IGuiScreen,
    name: CharSequence,
    uiSettingGroup: UiSettingGroup,
    config: AbstractConfig<out Nameable> = GuiConfig
) : Component(screen, name, uiSettingGroup, config) {

    // Interactive info
    protected var lastMousePos = Vec2f.ZERO
    protected var lastClickPos = Vec2f.ZERO
    var mouseState = MouseState.NONE
        private set(value) {
            prevState = field
            lastStateUpdateTime = System.currentTimeMillis()
            field = value
        }
    protected var prevState = MouseState.NONE; private set
    protected var lastStateUpdateTime = System.currentTimeMillis(); private set

    override fun onDisplayed() {
        super.onDisplayed()
        mouseState = MouseState.NONE
        prevState = MouseState.NONE
        lastStateUpdateTime = System.currentTimeMillis()
    }

    // Interactive methods
    open fun onMouseInput(mousePos: Vec2f) {
        lastMousePos = mousePos
    }

    open fun onHover(mousePos: Vec2f) {
        mouseState = MouseState.HOVER
    }

    open fun onLeave(mousePos: Vec2f) {
        mouseState = MouseState.NONE
    }

    open fun onClick(mousePos: Vec2f, buttonId: Int) {
        mouseState = MouseState.CLICK
    }

    open fun onRelease(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        mouseState = MouseState.HOVER
    }

    open fun onDrag(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        mouseState = MouseState.DRAG
        lastClickPos = clickPos
    }

    open fun onKeyInput(keyCode: Int, keyState: Boolean) {

    }
}