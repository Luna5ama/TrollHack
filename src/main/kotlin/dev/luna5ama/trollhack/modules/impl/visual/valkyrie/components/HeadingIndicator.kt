package dev.luna5ama.trollhack.modules.impl.visual.valkyrie.components

import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.Dimensions
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.FlightComputer
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.HudComponent
import dev.luna5ama.trollhack.utils.NonNullContext

class HeadingIndicator(private val computer: FlightComputer, private val dim: Dimensions) : HudComponent() {
    context(ctx: NonNullContext)
    override fun render(partial: Float): Unit = ctx.run {
        val left: Float = dim.lFrame
        val right: Float = dim.rFrame
        val top: Float = dim.tFrame - 10

        val yText = top - 7
        val northOffset: Float = computer.heading * dim.degreesPerPixel
        val xNorth: Float = dim.xMid - northOffset

        drawFont("%03d".format(i(wrapHeading(computer.heading).toDouble())), dim.xMid - 8, yText)
        drawBox(dim.xMid - 15, yText - 1.5f, 30f, 10f)

        drawPointer(dim.xMid, top + 10, 0f)
        var i = -540
        while (i < 540) {
            val x: Float = (i * dim.degreesPerPixel) + xNorth
            if (x < left || x > right) {
                i = i + 5
                continue
            }

            if (i % 15 == 0) {
                if (i % 90 == 0) {
                    drawFont(headingToDirection(i), x - 2, yText + 10)
                    drawFont(headingToAxis(i), x - 8, yText + 20)
                } else {
                    drawVerticalLine(x, top + 3, top + 10)
                }

                if (x <= dim.xMid - 26 || x >= dim.xMid + 26) {
                    drawFont("%03d".format(i(wrapHeading(i.toFloat()).toDouble())), x - 8, yText)
                }
            } else {
                drawVerticalLine(x, top + 6, top + 10)
            }
            i = i + 5
        }
    }

    private fun headingToDirection(degrees: Int): String {
        var degrees = degrees
        degrees = i(wrapHeading(degrees.toFloat()).toDouble())
        return when (degrees) {
            0, 360 -> "N"
            90 -> "E"
            180 -> "S"
            270 -> "W"
            else -> ""
        }
    }

    private fun headingToAxis(degrees: Int): String {
        var degrees = degrees
        degrees = i(wrapHeading(degrees.toFloat()).toDouble())
        return when (degrees) {
            0, 360 -> "-Z"
            90 -> "+X"
            180 -> "+Z"
            270 -> "-X"
            else -> ""
        }
    }
}
