package cum.xiaro.trollhack.gui.rgui.component

import cum.xiaro.trollhack.util.math.vector.Vec2f

class Button(
    name: CharSequence,
    private val action: (Button) -> Unit,
    description: CharSequence = "",
    visibility: ((() -> Boolean))? = null
) : BooleanSlider(name, 0.0f, description, visibility) {
    override fun onClick(mousePos: Vec2f, buttonId: Int) {
        super.onClick(mousePos, buttonId)
        value = 1.0f
    }

    override fun onRelease(mousePos: Vec2f, buttonId: Int) {
        super.onRelease(mousePos, buttonId)
        if (prevState != MouseState.DRAG) {
            value = 0.0f
            action(this)
        }
    }

    override fun onLeave(mousePos: Vec2f) {
        super.onLeave(mousePos)
        value = 0.0f
    }
}