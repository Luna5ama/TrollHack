package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.manager.AbstractManager
import dev.luna5ama.trollhack.modules.impl.client.Colors
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.color.ColorUtils
import java.awt.Color

internal object GuiManager : AbstractManager() {
    val rainbow get() = Colors.rainbow
    var rainbowDelay = 40
    var rainbowSaturation = 165
    var rainbowBrightness = 255
    var gradientIntensity = 50
    val color: Color get() = Color(Colors.red, Colors.green, Colors.blue)
    val background: Color get() = Color(Colors.bRed, Colors.bGreen, Colors.bBlue, Colors.bAlpha)
    val fontColor: Color get() = Color.WHITE

    val backgroundRGBA get() = ColorRGBA(background)

    fun getColor(add: Int = 0): ColorRGBA {
        return if (rainbow) ColorRGBA(getRainbow(add)) else Colors.color
    }

    fun getRainbow(add: Int = 0): Color {
        return ColorUtils.rainbow(rainbowDelay + add, rainbowSaturation, rainbowBrightness)
    }
}