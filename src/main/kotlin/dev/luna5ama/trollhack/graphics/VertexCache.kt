package dev.luna5ama.trollhack.graphics

import dev.luna5ama.trollhack.utils.math.primitive.Angle
import dev.luna5ama.trollhack.utils.math.vectors.Vec2d

class VertexCache(private val points: List<Vec2d>) : List<Vec2d> by points {
    fun transform(x: Double, y: Double): VertexCache {
        return VertexCache(points.map { it.plus(x, y) })
    }

    fun transform(vec: Vec2d): VertexCache {
        return VertexCache(points.map { it.plus(vec) })
    }

    fun scale(x: Double, y: Double) = VertexCache(points.map { it.times(x, y) })
    fun scale(scale: Double) = VertexCache(points.map { it.times(scale) })

    fun closing() = VertexCache(points + points[0])

    fun scissor(start: Int, end: Int) = VertexCache(points.subList(start, end))

    companion object {
        private val UNIT_CIRCLE: VertexCache by lazy {
            val start = Vec2d(0.0, 1.0)
            val points = mutableListOf<Vec2d>()
            for (i in 0..<720) {
                points.add(start.rotate(Angle(i.toDouble() / 2).toRadius()))
            }
            VertexCache(points)
        }

        fun createCircle(o: Vec2d, r: Double, segments: Int): VertexCache {
            if (segments == 0) return createCircle(o, r)
            val start = Vec2d(0.0, 1.0)
            val points = mutableListOf<Vec2d>()
            for (i in 0..<segments) {
                points.add(start.rotate(Angle(i.toDouble() / (segments / 360.0)).toRadius()))
            }
            return VertexCache(points).closing()
        }

        fun createCircle(o: Vec2d, r: Double) = UNIT_CIRCLE.scale(r).transform(o).closing()
    }
}