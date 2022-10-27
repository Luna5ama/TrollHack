package me.luna.trollhack.gui.rgui.component

import me.luna.trollhack.util.math.vector.Vec2f

class Button(
    name: CharSequence,
    private val action: (Button) -> Unit,
    description: CharSequence = "",
    visibility: ((() -> Boolean))? = null
) : BooleanSlider(name, description, visibility) {
    private var state = false

    override val progress: Float
        get() = if (state) 1.0f else 0.0f

    override fun onClick(mousePos: Vec2f, buttonId: Int) {
        super.onClick(mousePos, buttonId)
        state = true
    }

    override fun onRelease(mousePos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, buttonId)
        if (prevState != MouseState.DRAG) {
            state = false
            action(this)
        }
    }

    override fun onLeave(mousePos: Vec2f) {
        super.onLeave(mousePos)
        state = false
    }
}