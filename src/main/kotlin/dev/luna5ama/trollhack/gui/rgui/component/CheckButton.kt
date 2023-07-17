package dev.luna5ama.trollhack.gui.rgui.component

import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.gui.rgui.MouseState
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import kotlin.math.abs

open class CheckButton(
    screen: IGuiScreen,
    name: CharSequence,
    description: CharSequence = "",
    visibility: ((() -> Boolean))? = null
) : BooleanSlider(screen, name, description, visibility) {
    open var state = false

    override val progress: Float
        get() {
            if (!visible) {
                return 0.0f
            }

            if (mouseState != MouseState.DRAG) {
                return if (state) 1.0f else 0.0f
            }

            return Float.NaN
        }

    override fun onRelease(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)

        if (prevState == MouseState.DRAG && abs(mousePos.x - lastClickPos.x) > 16.0f) {
            state = renderProgress.current > if (state) 0.7f else 0.3f
        } else if (mousePos.x in -2.0f..renderWidth + 2.0f && mousePos.y in -2.0f..renderHeight + 2.0f) {
            state = !state
        }
    }

    override fun onDrag(mousePos: Vec2f, clickPos: Vec2f, buttonId: Int) {
        super.onDrag(mousePos, clickPos, buttonId)
        val prevProgress = if (state) 1.0f else 0.0f
        renderProgress.update((prevProgress + (mousePos.x - clickPos.x) / width).coerceIn(0.0f, 1.0f))
    }
}