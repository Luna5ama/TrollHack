package dev.luna5ama.trollhack.modules.impl.visual.valkyrie.components

import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.Dimensions
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.FlightComputer
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.HudComponent
import dev.luna5ama.trollhack.utils.NonNullContext
import java.lang.String
import kotlin.Float
import kotlin.math.roundToInt
import kotlin.times

class ElytraHealthIndicator(private val computer: FlightComputer, private val dim: Dimensions) : HudComponent() {
    context(NonNullContext)
    override fun render(partial: Float) {
        val x = dim.wScreen * 0.5f
        val y = dim.hScreen * 0.8f

        drawBox(x - 3.5f, y - 1.5f, 30f, 10f)
        drawFont("E", x - 10, y)
        drawFont(computer.elytraHealth.toDouble().roundToInt().toString() + "%", x, y)
    }
}