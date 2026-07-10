package dev.luna5ama.trollhack.graphics.skia

import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Data
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontEdging
import org.jetbrains.skia.FontHinting
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Shader
import org.jetbrains.skia.Typeface
import kotlin.math.max
import kotlin.math.min

/**
 * Lightweight immediate drawing facade over a frame-owned Skia canvas.
 *
 * Calls only record Skia commands. Resource submission remains owned by
 * [SkiaMinecraftBridge], so a module cannot accidentally flush the batch.
 */
class SkiaDrawScope internal constructor(val canvas: Canvas) {
    fun saved(block: SkiaDrawScope.() -> Unit) {
        val saveCount = canvas.save()
        try {
            block()
        } finally {
            canvas.restoreToCount(saveCount)
        }
    }

    fun translate(x: Float, y: Float) {
        canvas.translate(x, y)
    }

    fun rotate(degrees: Float, x: Float = 0f, y: Float = 0f) {
        canvas.rotate(degrees, x, y)
    }

    fun scale(x: Float, y: Float = x) {
        canvas.scale(x, y)
    }

    fun point(x: Float, y: Float, size: Float, color: ColorRGBA) {
        canvas.drawPoint(x, y, stroke(color, size).apply { strokeCap = PaintStrokeCap.ROUND })
    }

    fun line(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        width: Float,
        color: ColorRGBA
    ) {
        canvas.drawLine(startX, startY, endX, endY, stroke(color, width))
    }

    fun polyline(points: FloatArray, width: Float, color: ColorRGBA, close: Boolean = false) {
        if (points.size < 4) return
        var index = 2
        while (index + 1 < points.size) {
            canvas.drawLine(
                points[index - 2],
                points[index - 1],
                points[index],
                points[index + 1],
                stroke(color, width)
            )
            index += 2
        }
        if (close) {
            canvas.drawLine(
                points[points.size - 2],
                points[points.size - 1],
                points[0],
                points[1],
                stroke(color, width)
            )
        }
    }

    fun triangle(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        color: ColorRGBA
    ) {
        pathBuilder.reset()
            .moveTo(x1, y1)
            .lineTo(x2, y2)
            .lineTo(x3, y3)
            .closePath()
        pathBuilder.detach().use { canvas.drawPath(it, fill(color)) }
    }

    fun rect(left: Float, top: Float, right: Float, bottom: Float, color: ColorRGBA) {
        canvas.drawRect(min(left, right), min(top, bottom), max(left, right), max(top, bottom), fill(color))
    }

    fun rectOutline(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        width: Float,
        color: ColorRGBA
    ) {
        canvas.drawRect(min(left, right), min(top, bottom), max(left, right), max(top, bottom), stroke(color, width))
    }

    fun arc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startDegrees: Float,
        sweepDegrees: Float,
        width: Float,
        color: ColorRGBA
    ) {
        canvas.drawArc(left, top, right, bottom, startDegrees, sweepDegrees, false, stroke(color, width))
    }

    fun text(
        text: String,
        x: Float,
        y: Float,
        size: Float = DEFAULT_TEXT_SIZE,
        color: ColorRGBA = ColorRGBA.WHITE,
        shadow: Boolean = false
    ) {
        val cleanText = stripFormatting(text)
        val font = font(size)
        val baseline = y - font.metrics.ascent
        if (shadow) {
            canvas.drawString(cleanText, x + 1f, baseline + 1f, font, fill(ColorRGBA(0, 0, 0, color.a)))
        }
        canvas.drawString(cleanText, x, baseline, font, fill(color))
    }

    fun centeredText(
        text: String,
        centerX: Float,
        y: Float,
        size: Float = DEFAULT_TEXT_SIZE,
        color: ColorRGBA = ColorRGBA.WHITE,
        shadow: Boolean = false
    ) {
        text(text, centerX - measureText(text, size) * 0.5f, y, size, color, shadow)
    }

    fun gradientText(
        text: String,
        x: Float,
        y: Float,
        size: Float,
        startColor: ColorRGBA,
        endColor: ColorRGBA,
        shadow: Boolean = false
    ) {
        val cleanText = stripFormatting(text)
        val font = font(size)
        val width = font.measureTextWidth(cleanText)
        val baseline = y - font.metrics.ascent
        if (shadow) {
            canvas.drawString(cleanText, x + 1f, baseline + 1f, font, fill(ColorRGBA(0, 0, 0, 180)))
        }
        Shader.makeLinearGradient(
            x,
            y,
            x + width,
            y,
            intArrayOf(startColor.argb, endColor.argb),
            null
        ).use { shader ->
            fillPaint.shader = shader
            canvas.drawString(cleanText, x, baseline, font, fillPaint)
            fillPaint.shader = null
        }
    }

    fun measureText(text: String, size: Float = DEFAULT_TEXT_SIZE): Float =
        font(size).measureTextWidth(stripFormatting(text))

    fun textHeight(size: Float = DEFAULT_TEXT_SIZE): Float = font(size).spacing

    companion object {
        const val DEFAULT_TEXT_SIZE = 12f

        private val formattingCode = Regex("\\u00a7.")
        private val fillPaint = Paint().apply {
            mode = PaintMode.FILL
            isAntiAlias = true
        }
        private val strokePaint = Paint().apply {
            mode = PaintMode.STROKE
            isAntiAlias = true
            strokeCap = PaintStrokeCap.SQUARE
            strokeJoin = PaintStrokeJoin.MITER
        }
        private val pathBuilder = PathBuilder()
        private val typeface: Typeface by lazy {
            val bytes = SkiaDrawScope::class.java.getResourceAsStream("/assets/trollhack/MicrosoftYahei.ttf")
                ?.use { it.readBytes() }
            if (bytes == null) {
                FontMgr.default.matchFamilyStyle(null, org.jetbrains.skia.FontStyle.NORMAL)
                    ?: Typeface.makeEmpty()
            } else {
                Data.makeFromBytes(bytes).use { FontMgr.default.makeFromData(it) }
                    ?: Typeface.makeEmpty()
            }
        }
        private val fonts = HashMap<Float, Font>()

        private val ColorRGBA.argb: Int
            get() = (a shl 24) or (r shl 16) or (g shl 8) or b

        private fun stripFormatting(text: String) = formattingCode.replace(text, "")

        private fun fill(color: ColorRGBA): Paint = fillPaint.apply {
            shader = null
            this.color = color.argb
        }

        private fun stroke(color: ColorRGBA, width: Float): Paint = strokePaint.apply {
            shader = null
            this.color = color.argb
            strokeWidth = width.coerceAtLeast(0.25f)
            strokeCap = PaintStrokeCap.SQUARE
        }

        private fun font(size: Float): Font = fonts.getOrPut(size) {
            Font(typeface, size).apply {
                edging = FontEdging.SUBPIXEL_ANTI_ALIAS
                hinting = FontHinting.NORMAL
                isSubpixel = true
            }
        }
    }
}
