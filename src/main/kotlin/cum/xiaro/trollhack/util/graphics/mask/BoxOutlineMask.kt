package cum.xiaro.trollhack.util.graphics.mask

@JvmInline
value class BoxOutlineMask private constructor(val mask: Int) {
    val isEmpty get() = mask == 0

    infix fun or(other: BoxOutlineMask): BoxOutlineMask {
        return BoxOutlineMask(this.mask or other.mask)
    }

    operator fun plus(other: BoxOutlineMask): BoxOutlineMask {
        return this or other
    }

    operator fun minus(other: BoxOutlineMask): BoxOutlineMask {
        return BoxOutlineMask(this.mask and other.mask.inv())
    }

    operator fun contains(other: BoxOutlineMask): Boolean {
        return this.mask.inv() and other.mask == 0
    }

    fun toVertexMask(): BoxVertexMask {
        var mask = BoxVertexMask.EMPTY

        if (contains(DOWN_NORTH)) {
            mask = mask or BoxVertexMask.DOWN_NORTH
        }

        if (contains(DOWN_SOUTH)) {
            mask = mask or BoxVertexMask.DOWN_SOUTH
        }

        if (contains(DOWN_WEST)) {
            mask = mask or BoxVertexMask.DOWN_WEST
        }

        if (contains(DOWN_EAST)) {
            mask = mask or BoxVertexMask.DOWN_EAST
        }


        if (contains(UP_NORTH)) {
            mask = mask or BoxVertexMask.UP_NORTH
        }

        if (contains(UP_SOUTH)) {
            mask = mask or BoxVertexMask.UP_SOUTH
        }

        if (contains(UP_WEST)) {
            mask = mask or BoxVertexMask.UP_WEST
        }

        if (contains(UP_EAST)) {
            mask = mask or BoxVertexMask.UP_EAST
        }


        if (contains(NORTH_WEST)) {
            mask = mask or BoxVertexMask.NORTH_WEST
        }

        if (contains(NORTH_EAST)) {
            mask = mask or BoxVertexMask.NORTH_EAST
        }

        if (contains(SOUTH_WEST)) {
            mask = mask or BoxVertexMask.SOUTH_WEST
        }

        if (contains(SOUTH_EAST)) {
            mask = mask or BoxVertexMask.SOUTH_EAST
        }

        return mask
    }

    companion object {
        val EMPTY = BoxOutlineMask(0)

        val DOWN_NORTH = BoxOutlineMask(1 shl 0)
        val DOWN_SOUTH = BoxOutlineMask(1 shl 1)
        val DOWN_WEST = BoxOutlineMask(1 shl 2)
        val DOWN_EAST = BoxOutlineMask(1 shl 3)

        val UP_NORTH = BoxOutlineMask(1 shl 4)
        val UP_SOUTH = BoxOutlineMask(1 shl 5)
        val UP_WEST = BoxOutlineMask(1 shl 6)
        val UP_EAST = BoxOutlineMask(1 shl 7)

        val NORTH_WEST = BoxOutlineMask(1 shl 8)
        val NORTH_EAST = BoxOutlineMask(1 shl 9)
        val SOUTH_WEST = BoxOutlineMask(1 shl 10)
        val SOUTH_EAST = BoxOutlineMask(1 shl 11)

        val DOWN = DOWN_NORTH or DOWN_SOUTH or DOWN_WEST or DOWN_EAST
        val UP = UP_NORTH or UP_SOUTH or UP_WEST or UP_EAST
        val NORTH = DOWN_NORTH or UP_NORTH or NORTH_WEST or NORTH_EAST
        val SOUTH = DOWN_SOUTH or UP_SOUTH or SOUTH_WEST or SOUTH_EAST
        val WEST = DOWN_WEST or UP_WEST or NORTH_WEST or SOUTH_WEST
        val EAST = DOWN_EAST or UP_EAST or NORTH_EAST or SOUTH_EAST
        val ALL = DOWN or UP or NORTH or SOUTH or WEST or EAST
    }
}