package dev.luna5ama.trollhack.gui.rgui.component

open class BooleanSlider(
    name: CharSequence,
    description: CharSequence,
    visibility: (() -> Boolean)? = null
) : Slider(name, description, visibility)