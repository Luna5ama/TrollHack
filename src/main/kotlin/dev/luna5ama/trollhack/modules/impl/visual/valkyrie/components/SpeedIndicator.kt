package dev.luna5ama.trollhack.modules.impl.visual.valkyrie.components

import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.Dimensions
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.FlightComputer
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.HudComponent
import dev.luna5ama.trollhack.utils.NonNullContext

class SpeedIndicator(private val computer: FlightComputer, private val dim: Dimensions) : HudComponent() {
    context(NonNullContext)
    override fun render(partial: Float) {
        val top: Float = dim.tFrame
        val bottom: Float = dim.bFrame

        val left: Float = dim.lFrame - 2
        val right: Float = dim.lFrame
        val unitPerPixel = 30f

        val floorOffset = computer.speed * unitPerPixel
        val yFloor: Float = dim.yMid - floorOffset

        val xSpeedText = left - 5

        drawRightAlignedFont("%.2f".format(computer.speed), xSpeedText, dim.yMid - 3)
        drawBox(xSpeedText - 29.5f, dim.yMid - 4.5f, 30f, 10f)

        var i = 0f
        while (i <= 300) {
            val y: Float = dim.hScreen - i * unitPerPixel - yFloor
            if (y < top || y > (bottom - 5)) {
                i = i + 0.25f
                continue
            }

            if (i % 1 == 0f) {
                drawHorizontalLine(left - 2, right, y)
                if (y > dim.yMid + 7 || y < dim.yMid - 7) {
                    drawRightAlignedFont("%.0f".format(i), xSpeedText, y - 3)
                }
            }
            drawHorizontalLine(left, right, y)
            i = i + 0.25f
        }
    }
}