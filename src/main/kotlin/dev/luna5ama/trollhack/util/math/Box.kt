package dev.luna5ama.trollhack.util.math

class Box(
    var x1: Float,
    var y1: Float,
    var x2: Float,
    var y2: Float
) {
    fun contains(x: Float, y: Float): Boolean {
        return x in x1..x2 && y in y1..y2
    }
}