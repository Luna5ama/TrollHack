package dev.luna5ama.trollhack.modules.impl.visual.valkyrie

import dev.luna5ama.trollhack.RS
import dev.luna5ama.trollhack.graphics.buffer.Render2DUtils
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.matrix.rotatef
import dev.luna5ama.trollhack.graphics.matrix.scope
import dev.luna5ama.trollhack.graphics.matrix.translatef
import dev.luna5ama.trollhack.manager.managers.UnicodeFontManager
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.math.vectors.Vec3f
import kotlin.math.roundToInt

abstract class HudComponent {
    context(ctx: NonNullContext)
    abstract fun render(partial: Float)

    protected fun i(d: Double): Int {
        return d.roundToInt().toInt()
    }

    protected fun drawPointer(x: Float, y: Float, rot: Float) {
        RS.matrixLayer.scope {
            translatef(x, y, 0f)
            rotatef(rot + 45, Vec3f(0.0f, 0.0f, 1.0f))
            drawVerticalLine(0f, 0f, 5f)
            drawHorizontalLine(0f, 5f, 0f)
        }
    }

    protected fun wrapHeading(degrees: Float): Float {
        var degrees = degrees
        degrees = degrees % 360
        while (degrees < 0) {
            degrees += 360f
        }
        return degrees
    }

    protected fun drawFont(s: String, x: Float, y: Float) {
        drawFont(s, x, y, Valkyrie.color)
    }

    protected fun drawFont(s: String, x: Float, y: Float, color: ColorRGBA) {
        UnicodeFontManager.CURRENT_FONT.drawString(s, x, y - 3, color.awt)
    }

    protected fun getFontWidth(s: String) = UnicodeFontManager.CURRENT_FONT.getWidth(s)

    protected fun drawRightAlignedFont(s: String, x: Float, y: Float) {
        val w = getFontWidth(s)
        drawFont(s, x - w, y)
    }

    protected fun drawBox(x: Float, y: Float, w: Float, h: Float) {
        drawHorizontalLine(x, x + w, y)
        drawHorizontalLine(x, x + w, y + h)
        drawVerticalLine(x, y, y + h)
        drawVerticalLine(x + w, y, y + h)
    }

    protected fun drawHorizontalLineDashed(
        x1: Float, x2: Float, y: Float,
        dashCount: Int
    ) {
        val width = x2 - x1
        val segmentCount = dashCount * 2 - 1
        val dashSize = width / segmentCount
        for (i in 0..<segmentCount) {
            if (i % 2 != 0) {
                continue
            }
            val dx1 = i * dashSize + x1
            val dx2: Float = if (i == segmentCount - 1) {
                x2
            } else {
                ((i + 1) * dashSize) + x1
            }
            drawHorizontalLine(dx1, dx2, y)
        }
    }

    protected fun drawHorizontalLine(x1: Float, x2: Float, y: Float) {
        var x1 = x1
        var x2 = x2
        if (x2 < x1) {
            val i = x1
            x1 = x2
            x2 = i
        }
        fill(x1 - Valkyrie.halfThickness, y - Valkyrie.halfThickness, x2 + Valkyrie.halfThickness, y + Valkyrie.halfThickness)
    }

    protected fun drawVerticalLine(x: Float, y1: Float, y2: Float) {
        var y1 = y1
        var y2 = y2
        if (y2 < y1) {
            val i = y1
            y1 = y2
            y2 = i
        }

        fill(x - Valkyrie.halfThickness, y1 + Valkyrie.halfThickness, x + Valkyrie.halfThickness, y2 - Valkyrie.halfThickness)
    }

    companion object {
        fun fill(x1: Float, y1: Float, x2: Float, y2: Float) {
            var x1 = x1
            var y1 = y1
            var x2 = x2
            var y2 = y2
            var j: Float

            if (x1 < x2) {
                j = x1
                x1 = x2
                x2 = j
            }

            if (y1 < y2) {
                j = y1
                y1 = y2
                y2 = j
            }
            val color = Valkyrie.color
            Render2DUtils.drawRect(x1, y1, x2, y2, color)
        }
    }
}
