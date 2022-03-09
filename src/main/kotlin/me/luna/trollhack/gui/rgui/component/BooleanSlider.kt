package me.luna.trollhack.gui.rgui.component

import me.luna.trollhack.util.delegate.FrameValue
import me.luna.trollhack.util.graphics.Easing

open class BooleanSlider(
    name: CharSequence,
    value: Float,
    description: CharSequence,
    visibility: ((() -> Boolean))? = null
) : Slider(name, value, description, visibility) {
    override val renderProgress: Float by FrameValue { Easing.OUT_CIRC.incOrDecOpposite(Easing.toDelta(prevValue.lastUpdateTime, 200L), prevValue.value, this.value) }
}