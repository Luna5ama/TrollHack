package cum.xiaro.trollhack.util.graphics.color

import cum.xiaro.trollhack.util.graphics.ColorRGB
import java.awt.Color

class HueCycler(val cycles: Int) {
    private val hueMultiplier = 1f / cycles.toFloat()
    private val colorCycle: Array<Int> = Array(cycles) { i ->
        Color.HSBtoRGB(i * hueMultiplier, 1f, 1f)
    }
    private var index = 0

    fun reset() {
        set(0)
    }

    fun set(indexIn: Int) {
        index = indexIn
    }

    fun currentHex(): Int {
        return colorCycle[index]
    }

    fun currentRgba(alpha: Int): ColorRGB {
        val color = currentRgb()
        return color.alpha(alpha)
    }

    fun currentRgb(): ColorRGB {
        return ColorRGB(currentHex())
    }

    operator fun plus(plus: Int) {
        index += plus
        if (index >= cycles) index = 0
    }

    operator fun inc(): HueCycler {
        index++
        if (index >= cycles) index = 0
        return this
    }

    init {
        require(cycles > 0) { "cycles <= 0" }
    }
}