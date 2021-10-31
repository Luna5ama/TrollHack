package cum.xiaro.trollhack.gui.rgui.component

import cum.xiaro.trollhack.util.math.vector.Vec2f

class CheckButton(
    name: String,
    stateIn: Boolean,
    description: String = "",
    visibility: ((() -> Boolean))? = null
) : BooleanSlider(name, 0.0f, description, visibility) {
    init {
        value = if (stateIn) 1.0f else 0.0f
    }

    override fun onClick(mousePos: Vec2f, buttonId: Int) {
        super.onClick(mousePos, buttonId)
        value = if (value == 1.0f) 0.0f else 1.0f
    }
}