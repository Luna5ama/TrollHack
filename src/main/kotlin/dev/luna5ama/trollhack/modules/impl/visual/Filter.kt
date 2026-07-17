package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.Displayable

object Filter : Module("Filter", category = Category.RENDER) {
    private val mode by setting("Mode", Mode.SHADER)
    val color by setting("Base Color", ColorRGBA(70, 70, 150, 50))
    val isShaderMode get() = isEnabled && mode == Mode.SHADER
    val isLightMapMode get() = isEnabled && mode == Mode.LIGHT_MAP

    @JvmStatic
    fun lightMapArgb(): Int = (255 shl 24) or (color.r shl 16) or (color.g shl 8) or color.b

    enum class Mode(override val displayName: CharSequence) : Displayable {
        SHADER("Shader"), LIGHT_MAP("Light Map")
    }
}
