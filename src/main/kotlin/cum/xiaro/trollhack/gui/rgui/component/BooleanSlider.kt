package cum.xiaro.trollhack.gui.rgui.component

import cum.xiaro.trollhack.util.graphics.Easing
import cum.xiaro.trollhack.util.delegate.FrameValue

open class BooleanSlider(
    name: CharSequence,
    value: Float,
    description: CharSequence,
    visibility: ((() -> Boolean))? = null
) : Slider(name, value, description, visibility) {
    override val renderProgress: Float by FrameValue { Easing.OUT_CIRC.incOrDecOpposite(Easing.toDelta(prevValue.lastUpdateTime, 200L), prevValue.value, this.value) }
}