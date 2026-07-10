package dev.luna5ama.trollhack.graphics.buffer

import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBORenderer2D
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.utils.math.MathUtils
import dev.luna5ama.trollhack.utils.math.toRadian
import dev.luna5ama.trollhack.utils.math.vectors.Vec2d
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object Render2DUtils : Renderer2D by PMVBORenderer2D {

    fun drawPoint(
        x: Double,
        y: Double,
        size: Float = 1F,
        color: ColorRGBA,
    ) = drawPoint0(x.toFloat(), y.toFloat(), size, color)

    fun drawPoint(
        x: Int,
        y: Int,
        size: Float = 1F,
        color: ColorRGBA,
    ) = drawPoint0(x.toFloat(), y.toFloat(), size, color)

    fun drawPoint(
        x: Float,
        y: Float,
        size: Float = 1F,
        color: ColorRGBA,
    ) = drawPoint0(x, y, size, color)

    fun drawLine(
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double,
        width: Float = 1F,
        color1: ColorRGBA,
        color2: ColorRGBA = color1,
    ) = drawLine0(startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat(), width, color1, color2)

    fun drawLine(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        width: Float = 1F,
        color1: ColorRGBA,
        color2: ColorRGBA = color1,
    ) = drawLine0(
        startX.toFloat(),
        startY.toFloat(),
        endX.toFloat(),
        endY.toFloat(),
        width,
        color1,
        color2
    )

    fun drawLine(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        width: Float = 1F,
        color1: ColorRGBA,
        color2: ColorRGBA = color1,
    ) = drawLine0(
        startX,
        startY,
        endX,
        endY,
        width,
        color1,
        color2
    )

    fun drawLine(
        start: Vec2d,
        end: Vec2d,
        width: Float = 1F,
        color1: ColorRGBA,
        color2: ColorRGBA = color1,
    ) = drawLine0(start.x.toFloat(), start.y.toFloat(), end.x.toFloat(), end.y.toFloat(), width, color1, color2)

    fun drawLine(
        start: Vec2f,
        end: Vec2f,
        width: Float = 1F,
        color1: ColorRGBA,
        color2: ColorRGBA = color1,
    ) = drawLine0(
        start.x,
        start.y,
        end.x,
        end.y,
        width,
        color1,
        color2
    )

    fun drawLinesStrip(
        vertexArray: Array<Vec2d>,
        width: Float = 1F,
        color: ColorRGBA,
    ) = drawLinesStrip0(Array(vertexArray.size) { vertexArray[it].toVec2f() }, width, color)

    fun drawLinesLoop(
        vertexArray: Array<Vec2d>,
        width: Float = 1F,
        color: ColorRGBA,
    ) = drawLinesLoop0(Array(vertexArray.size) { vertexArray[it].toVec2f() }, width, color)

    fun drawLinesStrip(
        vertexArray: Array<Vec2f>,
        width: Float = 1F,
        color: ColorRGBA,
    ) = drawLinesStrip0(vertexArray, width, color)

    fun drawLinesLoop(
        vertexArray: Array<Vec2f>,
        width: Float = 1F,
        color: ColorRGBA,
    ) = drawLinesLoop0(vertexArray, width, color)

    fun drawTriangle(
        pos1X: Double,
        pos1Y: Double,
        pos2X: Double,
        pos2Y: Double,
        pos3X: Double,
        pos3Y: Double,
        color: ColorRGBA,
    ) = drawTriangle0(
        pos1X.toFloat(),
        pos1Y.toFloat(),
        pos2X.toFloat(),
        pos2Y.toFloat(),
        pos3X.toFloat(),
        pos3Y.toFloat(),
        color
    )

    fun drawTriangle(
        pos1X: Int,
        pos1Y: Int,
        pos2X: Int,
        pos2Y: Int,
        pos3X: Int,
        pos3Y: Int,
        color: ColorRGBA,
    ) = drawTriangle0(
        pos1X.toFloat(),
        pos1Y.toFloat(),
        pos2X.toFloat(),
        pos2Y.toFloat(),
        pos3X.toFloat(),
        pos3Y.toFloat(),
        color
    )

    fun drawTriangle(
        pos1X: Float,
        pos1Y: Float,
        pos2X: Float,
        pos2Y: Float,
        pos3X: Float,
        pos3Y: Float,
        color: ColorRGBA,
    ) = drawTriangle0(
        pos1X,
        pos1Y,
        pos2X,
        pos2Y,
        pos3X,
        pos3Y,
        color
    )

    fun drawTriangle(
        pos1: Vec2d,
        pos2: Vec2d,
        pos3: Vec2d,
        color: ColorRGBA,
    ) = drawTriangle0(
        pos1.x.toFloat(),
        pos1.y.toFloat(),
        pos2.x.toFloat(),
        pos2.y.toFloat(),
        pos3.x.toFloat(),
        pos3.y.toFloat(),
        color
    )

    fun drawTriangle(
        pos1: Vec2f,
        pos2: Vec2f,
        pos3: Vec2f,
        color: ColorRGBA,
    ) = drawTriangle0(
        pos1.x,
        pos1.y,
        pos2.x,
        pos2.y,
        pos3.x,
        pos3.y,
        color
    )

    fun drawTriangleFan(
        centerX: Double,
        centerY: Double,
        vertices: Array<Vec2f>,
        centerColor: ColorRGBA,
        color: ColorRGBA = centerColor
    ) = drawTriangleFan0(centerX.toFloat(), centerY.toFloat(), vertices, centerColor, color)

    fun drawTriangleFan(
        centerX: Float,
        centerY: Float,
        vertices: Array<Vec2f>,
        centerColor: ColorRGBA,
        color: ColorRGBA = centerColor
    ) = drawTriangleFan0(centerX, centerY, vertices, centerColor, color)

    fun drawTriangleOutline(
        pos1X: Double,
        pos1Y: Double,
        pos2X: Double,
        pos2Y: Double,
        pos3X: Double,
        pos3Y: Double,
        width: Float = 1F,
        color: ColorRGBA,
    ) = drawTriangleOutline0(
        pos1X.toFloat(),
        pos1Y.toFloat(),
        pos2X.toFloat(),
        pos2Y.toFloat(),
        pos3X.toFloat(),
        pos3Y.toFloat(),
        width,
        color
    )

    fun drawTriangleOutline(
        pos1X: Int,
        pos1Y: Int,
        pos2X: Int,
        pos2Y: Int,
        pos3X: Int,
        pos3Y: Int,
        width: Float = 1F,
        color: ColorRGBA,
    ) = drawTriangleOutline0(
        pos1X.toFloat(),
        pos1Y.toFloat(),
        pos2X.toFloat(),
        pos2Y.toFloat(),
        pos3X.toFloat(),
        pos3Y.toFloat(),
        width,
        color
    )

    fun drawTriangleOutline(
        pos1X: Float,
        pos1Y: Float,
        pos2X: Float,
        pos2Y: Float,
        pos3X: Float,
        pos3Y: Float,
        width: Float = 1F,
        color: ColorRGBA,
    ) = drawTriangleOutline0(
        pos1X,
        pos1Y,
        pos2X,
        pos2Y,
        pos3X,
        pos3Y,
        width,
        color
    )

    fun drawTriangleOutline(
        pos1: Vec2d,
        pos2: Vec2d,
        pos3: Vec2d,
        width: Float = 1F,
        color: ColorRGBA,
    ) = drawTriangleOutline0(
        pos1.x.toFloat(),
        pos1.y.toFloat(),
        pos2.x.toFloat(),
        pos2.y.toFloat(),
        pos3.x.toFloat(),
        pos3.y.toFloat(),
        width,
        color
    )

    fun drawTriangleOutline(
        pos1: Vec2f,
        pos2: Vec2f,
        pos3: Vec2f,
        width: Float = 1F,
        color: ColorRGBA,
    ) = drawTriangleOutline0(
        pos1.x,
        pos1.y,
        pos2.x,
        pos2.y,
        pos3.x,
        pos3.y,
        width,
        color
    )

    fun drawRect(
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double,
        color: ColorRGBA,
    ) = drawRect0(startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat(), color)

    fun drawRect(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        color: ColorRGBA,
    ) = drawRect0(startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat(), color)

    fun drawRect(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        color: ColorRGBA,
    ) = drawRect0(startX, startY, endX, endY, color)

    fun drawRect(
        start: Vec2d,
        end: Vec2d,
        color: ColorRGBA,
    ) = drawRect0(start.x.toFloat(), start.y.toFloat(), end.x.toFloat(), end.y.toFloat(), color)

    fun drawRect(
        start: Vec2f,
        end: Vec2f,
        color: ColorRGBA,
    ) = drawRect0(start.x, start.y, end.x, end.y, color)

    fun drawRect(
        x: Float,
        y: Float,
        color: ColorRGBA
    ) = drawRect0(0f, 0f, x, y, color)

    fun drawGradientRect(
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double,
        color1: ColorRGBA,
        color2: ColorRGBA,
        color3: ColorRGBA,
        color4: ColorRGBA,
    ) = drawGradientRect0(
        startX.toFloat(),
        startY.toFloat(),
        endX.toFloat(),
        endY.toFloat(),
        color1,
        color2,
        color3,
        color4
    )

    fun drawGradientRect(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        color1: ColorRGBA,
        color2: ColorRGBA,
        color3: ColorRGBA,
        color4: ColorRGBA,
    ) = drawGradientRect0(
        startX.toFloat(),
        startY.toFloat(),
        endX.toFloat(),
        endY.toFloat(),
        color1,
        color2,
        color3,
        color4
    )

    fun drawGradientRect(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        color1: ColorRGBA,
        color2: ColorRGBA,
        color3: ColorRGBA,
        color4: ColorRGBA,
    ) = drawGradientRect0(
        startX,
        startY,
        endX,
        endY,
        color1,
        color2,
        color3,
        color4
    )

    fun drawGradientRect(
        start: Vec2d,
        end: Vec2d,
        color1: ColorRGBA,
        color2: ColorRGBA,
        color3: ColorRGBA,
        color4: ColorRGBA,
    ) = drawGradientRect0(
        start.x.toFloat(),
        start.y.toFloat(),
        end.x.toFloat(),
        end.y.toFloat(),
        color1,
        color2,
        color3,
        color4
    )

    fun drawGradientRect(
        start: Vec2f,
        end: Vec2f,
        color1: ColorRGBA,
        color2: ColorRGBA,
        color3: ColorRGBA,
        color4: ColorRGBA,
    ) = drawGradientRect0(
        start.x,
        start.y,
        end.x,
        end.y,
        color1,
        color2,
        color3,
        color4
    )

    fun drawHorizontalRect(
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawGradientRect0(
        startX.toFloat(),
        startY.toFloat(),
        endX.toFloat(),
        endY.toFloat(),
        endColorRGBA,
        startColor,
        startColor,
        endColorRGBA
    )

    fun drawHorizontalRect(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawGradientRect0(
        startX.toFloat(),
        startY.toFloat(),
        endX.toFloat(),
        endY.toFloat(),
        endColorRGBA,
        startColor,
        startColor,
        endColorRGBA
    )

    fun drawHorizontalRect(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawGradientRect0(
        startX,
        startY,
        endX,
        endY,
        endColorRGBA,
        startColor,
        startColor,
        endColorRGBA
    )

    fun drawHorizontalRect(
        start: Vec2d,
        end: Vec2d,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawGradientRect0(
        start.x.toFloat(),
        start.y.toFloat(),
        end.x.toFloat(),
        end.y.toFloat(),
        endColorRGBA,
        startColor,
        startColor,
        endColorRGBA
    )

    fun drawHorizontalRect(
        start: Vec2f,
        end: Vec2f,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawGradientRect0(
        start.x,
        start.y,
        end.x,
        end.y,
        endColorRGBA,
        startColor,
        startColor,
        endColorRGBA
    )

    fun drawVerticalRect(
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawGradientRect0(
        startX.toFloat(),
        startY.toFloat(),
        endX.toFloat(),
        endY.toFloat(),
        startColor,
        startColor,
        endColorRGBA,
        endColorRGBA
    )

    fun drawVerticalRect(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawGradientRect0(
        startX.toFloat(),
        startY.toFloat(),
        endX.toFloat(),
        endY.toFloat(),
        startColor,
        startColor,
        endColorRGBA,
        endColorRGBA
    )

    fun drawVerticalRect(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawGradientRect0(
        startX,
        startY,
        endX,
        endY,
        startColor,
        startColor,
        endColorRGBA,
        endColorRGBA
    )

    fun drawVerticalRect(
        start: Vec2d,
        end: Vec2d,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawGradientRect0(
        start.x.toFloat(),
        start.y.toFloat(),
        end.x.toFloat(),
        end.y.toFloat(),
        startColor,
        startColor,
        endColorRGBA,
        endColorRGBA
    )

    fun drawVerticalRect(
        start: Vec2f,
        end: Vec2f,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawGradientRect0(
        start.x,
        start.y,
        end.x,
        end.y,
        startColor,
        startColor,
        endColorRGBA,
        endColorRGBA
    )

    fun drawRectOutline(
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double,
        width: Float = 1F,
        color1: ColorRGBA,
        color2: ColorRGBA = color1,
        color3: ColorRGBA = color1,
        color4: ColorRGBA = color1,
    ) = drawRectOutline0(
        startX.toFloat(),
        startY.toFloat(),
        endX.toFloat(),
        endY.toFloat(),
        width,
        color1,
        color2,
        color3,
        color4
    )

    fun drawRectOutline(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        width: Float = 1F,
        color1: ColorRGBA,
        color2: ColorRGBA = color1,
        color3: ColorRGBA = color1,
        color4: ColorRGBA = color1,
    ) = drawRectOutline0(
        startX.toFloat(),
        startY.toFloat(),
        endX.toFloat(),
        endY.toFloat(),
        width,
        color1,
        color2,
        color3,
        color4
    )

    fun drawRectOutline(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        width: Float = 1F,
        color1: ColorRGBA,
        color2: ColorRGBA = color1,
        color3: ColorRGBA = color1,
        color4: ColorRGBA = color1,
    ) = drawRectOutline0(
        startX,
        startY,
        endX,
        endY,
        width,
        color1,
        color2,
        color3,
        color4
    )

    fun drawRectOutline(
        start: Vec2d,
        end: Vec2d,
        width: Float = 1F,
        color1: ColorRGBA,
        color2: ColorRGBA = color1,
        color3: ColorRGBA = color1,
        color4: ColorRGBA = color1,
    ) = drawRectOutline0(
        start.x.toFloat(),
        start.y.toFloat(),
        end.x.toFloat(),
        end.y.toFloat(),
        width,
        color1,
        color2,
        color3,
        color4
    )

    fun drawRectOutline(
        start: Vec2f,
        end: Vec2f,
        width: Float = 1F,
        color1: ColorRGBA,
        color2: ColorRGBA = color1,
        color3: ColorRGBA = color1,
        color4: ColorRGBA = color1,
    ) = drawRectOutline0(
        start.x,
        start.y,
        end.x,
        end.y,
        width,
        color1,
        color2,
        color3,
        color4
    )

    fun drawRectOutline(
        x: Float,
        y: Float,
        width: Float,
        color: ColorRGBA
    ) {
        drawRectOutline(0f, 0f, x, y, width, color)
    }

    fun drawHorizontalRectOutline(
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double,
        width: Float = 1F,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawRectOutline0(
        startX.toFloat(),
        startY.toFloat(),
        endX.toFloat(),
        endY.toFloat(),
        width,
        endColorRGBA,
        startColor,
        startColor,
        endColorRGBA
    )

    fun drawHorizontalRectOutline(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        width: Float = 1F,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawRectOutline0(
        startX.toFloat(),
        startY.toFloat(),
        endX.toFloat(),
        endY.toFloat(),
        width,
        endColorRGBA,
        startColor,
        startColor,
        endColorRGBA
    )

    fun drawHorizontalRectOutline(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        width: Float = 1F,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawRectOutline0(
        startX,
        startY,
        endX,
        endY,
        width,
        endColorRGBA,
        startColor,
        startColor,
        endColorRGBA
    )

    fun drawHorizontalRectOutline(
        start: Vec2d,
        end: Vec2d,
        width: Float = 1F,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawRectOutline0(
        start.x.toFloat(),
        start.y.toFloat(),
        end.x.toFloat(),
        end.y.toFloat(),
        width,
        endColorRGBA,
        startColor,
        startColor,
        endColorRGBA
    )

    fun drawHorizontalRectOutline(
        start: Vec2f,
        end: Vec2f,
        width: Float = 1F,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawRectOutline0(
        start.x,
        start.y,
        end.x,
        end.y,
        width,
        endColorRGBA,
        startColor,
        startColor,
        endColorRGBA
    )

    fun drawVerticalRectOutline(
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double,
        width: Float = 1F,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawRectOutline0(
        startX.toFloat(),
        startY.toFloat(),
        endX.toFloat(),
        endY.toFloat(),
        width,
        startColor,
        startColor,
        endColorRGBA,
        endColorRGBA
    )

    fun drawVerticalRectOutline(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        width: Float = 1F,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawRectOutline0(
        startX.toFloat(),
        startY.toFloat(),
        endX.toFloat(),
        endY.toFloat(),
        width,
        startColor,
        startColor,
        endColorRGBA,
        endColorRGBA
    )

    fun drawVerticalRectOutline(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        width: Float = 1F,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawRectOutline0(
        startX,
        startY,
        endX,
        endY,
        width,
        startColor,
        startColor,
        endColorRGBA,
        endColorRGBA
    )

    fun drawVerticalRectOutline(
        start: Vec2d,
        end: Vec2d,
        width: Float = 1F,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawRectOutline0(
        start.x.toFloat(),
        start.y.toFloat(),
        end.x.toFloat(),
        end.y.toFloat(),
        width,
        startColor,
        startColor,
        endColorRGBA,
        endColorRGBA
    )

    fun drawVerticalRectOutline(
        start: Vec2f,
        end: Vec2f,
        width: Float = 1F,
        startColor: ColorRGBA,
        endColorRGBA: ColorRGBA,
    ) = drawRectOutline0(
        start.x,
        start.y,
        end.x,
        end.y,
        width,
        startColor,
        startColor,
        endColorRGBA,
        endColorRGBA
    )

    fun drawArc(
        centerX: Double,
        centerY: Double,
        radius: Float,
        angleRange: ClosedFloatingPointRange<Float>,
        segments: Int = 0,
        color: ColorRGBA,
    ) = drawArc0(centerX.toFloat(), centerY.toFloat(), radius, angleRange, segments, color)

    fun drawArc(
        centerX: Int,
        centerY: Int,
        radius: Float,
        angleRange: ClosedFloatingPointRange<Float>,
        segments: Int = 0,
        color: ColorRGBA,
    ) = drawArc0(centerX.toFloat(), centerY.toFloat(), radius, angleRange, segments, color)

    fun drawArc(
        centerX: Float,
        centerY: Float,
        radius: Float,
        angleRange: ClosedFloatingPointRange<Float>,
        segments: Int = 0,
        color: ColorRGBA,
    ) = drawArc0(centerX, centerY, radius, angleRange, segments, color)

    fun drawArc(
        center: Vec2d,
        radius: Float,
        angleRange: ClosedFloatingPointRange<Float>,
        segments: Int = 0,
        color: ColorRGBA,
    ) = drawArc0(center.x.toFloat(), center.y.toFloat(), radius, angleRange, segments, color)

    fun drawArc(
        center: Vec2f,
        radius: Float,
        angleRange: ClosedFloatingPointRange<Float>,
        segments: Int = 0,
        color: ColorRGBA,
    ) = drawArc0(center.x, center.y, radius, angleRange, segments, color)

    fun drawArcOutline(
        centerX: Double,
        centerY: Double,
        radius: Float,
        angleRange: ClosedFloatingPointRange<Float>,
        segments: Int = 0,
        lineWidth: Float = 1f,
        color: ColorRGBA,
    ) = drawLinesStrip0(
        getArcVertices(centerX.toFloat(), centerY.toFloat(), radius, angleRange, segments),
        lineWidth,
        color
    )

    fun drawArcOutline(
        centerX: Int,
        centerY: Int,
        radius: Float,
        angleRange: ClosedFloatingPointRange<Float>,
        segments: Int = 0,
        lineWidth: Float = 1f,
        color: ColorRGBA,
    ) = drawLinesStrip0(
        getArcVertices(centerX.toFloat(), centerY.toFloat(), radius, angleRange, segments),
        lineWidth,
        color
    )

    fun drawArcOutline(
        centerX: Float,
        centerY: Float,
        radius: Float,
        angleRange: ClosedFloatingPointRange<Float>,
        segments: Int = 0,
        lineWidth: Float = 1f,
        color: ColorRGBA,
    ) = drawLinesStrip0(
        getArcVertices(centerX, centerY, radius, angleRange, segments),
        lineWidth,
        color
    )

    fun drawArcOutline(
        center: Vec2d,
        radius: Float,
        angleRange: ClosedFloatingPointRange<Float>,
        segments: Int = 0,
        lineWidth: Float = 1f,
        color: ColorRGBA,
    ) = drawLinesStrip0(
        getArcVertices(center.x.toFloat(), center.y.toFloat(), radius, angleRange, segments),
        lineWidth,
        color
    )

    fun drawArcOutline(
        center: Vec2f,
        radius: Float,
        angleRange: ClosedFloatingPointRange<Float>,
        segments: Int = 0,
        lineWidth: Float = 1f,
        color: ColorRGBA,
    ) = drawLinesStrip0(
        getArcVertices(center.x, center.y, radius, angleRange, segments),
        lineWidth,
        color
    )

    fun drawArcOutline(
        vertices: Array<Vec2d>,
        lineWidth: Float = 1f,
        color: ColorRGBA,
    ) = drawLinesStrip0(
        Array(vertices.size) { vertices[it].toVec2f() },
        lineWidth,
        color
    )

    fun drawArcOutline(
        vertices: Array<Vec2f>,
        lineWidth: Float = 1f,
        color: ColorRGBA,
    ) = drawLinesStrip0(
        vertices,
        lineWidth,
        color
    )

    fun getArcVertices(
        centerX: Float,
        centerY: Float,
        radius: Float,
        angleRange: ClosedFloatingPointRange<Float> = 0f..360f,
        segments: Int = 0,
    ): Array<Vec2f> {
        val range = max(angleRange.start, angleRange.endInclusive) - min(angleRange.start, angleRange.endInclusive)
        val seg = MathUtils.calcSegments(segments, radius, range)
        val segAngle = (range / seg.toFloat())

        return Array(seg + 1) {
            val angle = (it * segAngle + angleRange.start).toRadian()
            val unRounded = Vec2f(sin(angle), -cos(angle)).times(radius).plus(Vec2f(centerX, centerY))
            Vec2f(MathUtils.round(unRounded.x, 8), MathUtils.round(unRounded.y, 8))
        }
    }

}
