package dev.luna5ama.trollhack.modules.impl.visual.valkyrie.components

import dev.luna5ama.trollhack.RS
import dev.luna5ama.trollhack.graphics.matrix.rotatef
import dev.luna5ama.trollhack.graphics.matrix.scope
import dev.luna5ama.trollhack.graphics.matrix.translatef
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.Dimensions
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.FlightComputer
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.HudComponent
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.Valkyrie
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.math.fastFloorToFloat
import dev.luna5ama.trollhack.utils.math.vectors.Vec3f
import kotlin.math.abs
import kotlin.math.roundToInt


class PitchIndicator(private val computer: FlightComputer, private val dim: Dimensions) : HudComponent() {
    private val pitchData = PitchIndicatorData()

    context(ctx: NonNullContext)
    override fun render(partial: Float): Unit = ctx.run {
        pitchData.update(dim)

        val horizonOffset = computer.pitch * dim.degreesPerPixel
        val yHorizon = dim.yMid + horizonOffset

        val a = dim.yMid
        val b = dim.xMid

        val roll = computer.roll * (if (Valkyrie.reverseRoll) -1 else 1)

        RS.matrixLayer.scope {
            translatef(b, a, 0f)
            rotatef(roll, Vec3f(0f, 0f ,1f))
            translatef(-b, -a, 0f)

            drawLadder(yHorizon)


            drawReferenceMark(yHorizon, Valkyrie.optimumClimbAngle.toFloat())
            drawReferenceMark(yHorizon, Valkyrie.optimumGlideAngle.toFloat())

            pitchData.l1 -= pitchData.margin
            pitchData.r2 += pitchData.margin
            drawDegreeBar(0f, yHorizon)
        }
    }

    context(ctx: NonNullContext)
    private fun drawLadder(yHorizon: Float): Unit = ctx.run {
        var degreesPerBar = Valkyrie.degreesPerBar

        if (degreesPerBar < 1) {
            degreesPerBar = 20
        }

        var i = degreesPerBar
        while (i <= 90) {
            val offset = dim.degreesPerPixel * i
            drawDegreeBar(-i.toFloat(), yHorizon + offset)
            drawDegreeBar(i.toFloat(), yHorizon - offset)
            i = i + degreesPerBar
        }
    }

    private fun drawReferenceMark(yHorizon: Float, degrees: Float) {
        if (degrees == 0f) {
            return
        }

        val y: Float = (-degrees * dim.degreesPerPixel) + yHorizon

        if (y < dim.tFrame || y > dim.bFrame) {
            return
        }

        val width = (pitchData.l2 - pitchData.l1) * 0.45f
        val l1 = pitchData.l2 - width
        val r2 = pitchData.r1 + width

        drawHorizontalLineDashed(l1, pitchData.l2, y.fastFloorToFloat(), 3)
        drawHorizontalLineDashed(pitchData.r1, r2, y.fastFloorToFloat(), 3)
    }

    private fun drawDegreeBar(degree: Float, y: Float) {
        if (y < dim.tFrame || y > dim.bFrame) {
            return
        }

        val dashes = if (degree < 0) 4 else 1

        drawHorizontalLineDashed(pitchData.l1, pitchData.l2, y.fastFloorToFloat(), dashes)
        drawHorizontalLineDashed(pitchData.r1, pitchData.r2, y.fastFloorToFloat(), dashes)

        val sideTickHeight = if (degree >= 0) 5 else -5
        drawVerticalLine(pitchData.l1, y, y + sideTickHeight)
        drawVerticalLine(pitchData.r2, y, y + sideTickHeight)

        val fontVerticalOffset = if (degree >= 0) 0 else 6

        drawFont(
            abs(degree.toDouble()).roundToInt().toString(), pitchData.r2 + 6,
            y - fontVerticalOffset
        )

        drawFont(
            abs(degree.toDouble()).roundToInt().toString(), pitchData.l1 - 17,
            y - fontVerticalOffset
        )
    }

    private class PitchIndicatorData {
        var width: Float = 0f
        var mid: Float = 0f
        var margin: Float = 0f
        var sideWidth: Float = 0f
        var l1: Float = 0f
        var l2: Float = 0f
        var r1: Float = 0f
        var r2: Float = 0f

        fun update(dim: Dimensions) {
            width = (dim.wScreen / 3).roundToInt().toFloat()
            val left = width

            mid = ((width / 2) + left).roundToInt().toFloat()
            margin = (width * 0.3).roundToInt().toFloat()
            l1 = left + margin
            l2 = mid - 7
            sideWidth = l2 - l1
            r1 = mid + 8
            r2 = r1 + sideWidth
        }
    }
}
