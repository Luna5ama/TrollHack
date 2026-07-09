package dev.luna5ama.trollhack.modules.impl.visual.valkyrie.components

import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.Dimensions
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.FlightComputer
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.HudComponent
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.Valkyrie
import dev.luna5ama.trollhack.utils.NonNullContext
import kotlin.math.roundToInt

class FlightPathIndicator(private val computer: FlightComputer, private val dim: Dimensions) : HudComponent() {
    context(NonNullContext)
    override fun render(partial: Float) {
        val deltaPitch = computer.pitch - computer.flightPitch
        var deltaHeading = wrapHeading(computer.flightHeading) - wrapHeading(computer.heading)

        if (deltaHeading < -180) {
            deltaHeading += 360f
        }

        var y = dim.yMid
        var x = dim.xMid

        y += (deltaPitch * dim.degreesPerPixel).roundToInt()
        x += (deltaHeading * dim.degreesPerPixel).roundToInt()

        if (y < dim.tFrame || y > dim.bFrame || x < dim.lFrame || x > dim.rFrame) {
            return
        }

        val l = x - 3
        val r = x + 3
        val t = y - 3 - Valkyrie.halfThickness
        val b = y + 3 - Valkyrie.halfThickness

        drawVerticalLine(l, t, b)
        drawVerticalLine(r, t, b)

        drawHorizontalLine(l, r, t)
        drawHorizontalLine(l, r, b)

        drawVerticalLine(x, t - 5, t)
        drawHorizontalLine(l - 4, l, y - Valkyrie.halfThickness)
        drawHorizontalLine(r, r + 4, y - Valkyrie.halfThickness)
    }
}