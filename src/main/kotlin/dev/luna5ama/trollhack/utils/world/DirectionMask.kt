package dev.luna5ama.trollhack.utils.world

import net.minecraft.core.Direction
object DirectionMask {
    const val DOWN = 1 shl 0
    const val UP = 1 shl 1
    const val NORTH = 1 shl 2
    const val SOUTH = 1 shl 3
    const val WEST = 1 shl 4
    const val EAST = 1 shl 5
    const val ALL = DOWN or UP or NORTH or SOUTH or WEST or EAST

    fun getMaskForSide(side: Direction): Int {
        return when (side) {
            Direction.DOWN -> DOWN
            Direction.UP -> UP
            Direction.NORTH -> NORTH
            Direction.SOUTH -> SOUTH
            Direction.WEST -> WEST
            Direction.EAST -> EAST
        }
    }
}