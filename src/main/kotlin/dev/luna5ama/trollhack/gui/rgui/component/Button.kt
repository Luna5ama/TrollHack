package dev.luna5ama.trollhack.gui.rgui.component

import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.gui.rgui.MouseState
import dev.luna5ama.trollhack.util.math.vector.Vec2f

open class Button(
    screen: IGuiScreen,
    name: CharSequence,
    description: CharSequence = "",
    visibility: ((() -> Boolean))? = null
) : BooleanSlider(screen, name, description, visibility) {
    private val actions = mutableListOf<Action>()
    private var state = false

    fun action(action: Action): Button {
        actions.add(action)
        return this
    }

    override val progress: Float
        get() = if (state) 1.0f else 0.0f

    override fun onClick(mousePos: Vec2f, buttonId: Int) {
        super.onClick(mousePos, buttonId)
        state = true
    }

    override fun onRelease(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        if (state && prevState != MouseState.DRAG) {
            actions.forEach { it.invoke(mousePos, buttonId) }
        }
        state = false
    }

    override fun onLeave(mousePos: Vec2f) {
        super.onLeave(mousePos)
        state = false
    }

    fun interface Action {
        fun invoke(mousePos: Vec2f, buttonId: Int)
    }
}