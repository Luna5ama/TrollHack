package dev.luna5ama.trollhack.graphics.buffer

import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f

/**
 * Basic 2D love.xiguajerry.trollhack.graphics.animations.stuff
 */
interface Renderer2D {

    fun drawPoint0(x: Float, y: Float, size: Float, color: ColorRGBA)

    fun drawLine0(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        width: Float,
        color1: ColorRGBA,
        color2: ColorRGBA,
    )

    fun drawLinesStrip0(vertexArray: Array<Vec2f>, width: Float, color: ColorRGBA)

    fun drawLinesLoop0(vertexArray: Array<Vec2f>, width: Float, color: ColorRGBA)

    fun drawTriangle0(
        pos1X: Float,
        pos1Y: Float,
        pos2X: Float,
        pos2Y: Float,
        pos3X: Float,
        pos3Y: Float,
        color: ColorRGBA,
    )

    fun drawTriangleFan0(
        centerX: Float,
        centerY: Float,
        vertices: Array<Vec2f>,
        centerColor: ColorRGBA,
        color: ColorRGBA
    )

    fun drawTriangleOutline0(
        pos1X: Float,
        pos1Y: Float,
        pos2X: Float,
        pos2Y: Float,
        pos3X: Float,
        pos3Y: Float,
        width: Float,
        color: ColorRGBA,
    )

    fun drawRect0(startX: Float, startY: Float, endX: Float, endY: Float, color: ColorRGBA)

    fun drawGradientRect0(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        color1: ColorRGBA,
        color2: ColorRGBA,
        color3: ColorRGBA,
        color4: ColorRGBA,
    )

    fun drawRectOutline0(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        width: Float,
        color1: ColorRGBA,
        color2: ColorRGBA,
        color3: ColorRGBA,
        color4: ColorRGBA,
    )

    fun drawArc0(
        centerX: Float,
        centerY: Float,
        radius: Float,
        angleRange: ClosedFloatingPointRange<Float>,
        segments: Int,
        color: ColorRGBA,
    )

}