package dev.luna5ama.trollhack.modules.impl.visual.valkyrie.components

import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.Dimensions
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.FlightComputer
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.HudComponent
import dev.luna5ama.trollhack.utils.NonNullContext
import kotlin.math.roundToInt

class AltitudeIndicator(private val computer: FlightComputer, private val dim: Dimensions) : HudComponent() {
    context(ctx: NonNullContext)
    override fun render(partial: Float): Unit = ctx.run {
        val top = dim.tFrame
        val bottom = dim.bFrame

        val right = dim.rFrame + 2
        val left = dim.rFrame

        val blocksPerPixel = 1f

        val floorOffset = (computer.altitude * blocksPerPixel).toDouble().roundToInt().toFloat()
        val yFloor = dim.yMid - floorOffset
        val xAltText = right + 5

        drawHeightIndicator(left - 1, dim.yMid, bottom - dim.yMid)

        drawFont("%.0f".format(computer.altitude), xAltText, dim.yMid - 3)
        drawBox(xAltText - 2, dim.yMid - 4.5f, 28f, 10f)

        drawFont("G", xAltText - 10, bottom + 3)
        val heightText = computer.distanceFromGround.roundToInt().toString()
        drawFont(heightText, xAltText, bottom + 3)
        drawBox(xAltText - 2, bottom + 1.5f, 28f, 10f)

        var i = world.minY - 100
        while (i < 1000) {
            val y = (dim.hScreen - i * blocksPerPixel) - yFloor
            if (y < top || y > (bottom - 5)) {
                i = i + 10
                continue
            }

            if (i % 50 == 0) {
                drawHorizontalLine(left, right + 2, y)
                if (y > dim.yMid + 7 || y < dim.yMid - 7) {
                    drawRightAlignedFont(i.toString(), xAltText + getFontWidth("-00"), y - 3)
                }
            }
            drawHorizontalLine(left, right, y)
            i = i + 10
        }
    }

    context(ctx: NonNullContext)
    private fun drawHeightIndicator(x: Float, top: Float, h: Float): Unit = ctx.run {
        val bottom = top + h
        val blocksPerPixel: Float = h / (world.height + 64f)
        val yAlt = bottom - ((computer.altitude + 64) * blocksPerPixel).roundToInt()
        val yFloor = bottom - (64 * blocksPerPixel).roundToInt()

        drawVerticalLine(x, top - 1, bottom + 1)

        val yGroundLevel = bottom - (computer.groundLevel + 64f) * blocksPerPixel
        fill(x - 3, yGroundLevel + 2, x, yFloor)

        drawHorizontalLine(x - 6, x - 1, top)
        drawHorizontalLine(x - 6, x - 1, yFloor)
        drawHorizontalLine(x - 6, x - 1, bottom)

        drawPointer(x, yAlt, 90f)
    }
}
