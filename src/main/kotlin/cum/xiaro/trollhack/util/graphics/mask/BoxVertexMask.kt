package cum.xiaro.trollhack.util.graphics.mask

import net.minecraft.util.EnumFacing

@JvmInline
value class BoxVertexMask private constructor(val mask: Int) {
    val isEmpty get() = mask == 0

    infix fun or(other: BoxVertexMask): BoxVertexMask {
        return BoxVertexMask(this.mask or other.mask)
    }

    operator fun plus(other: BoxVertexMask): BoxVertexMask {
        return this or other
    }

    operator fun minus(other: BoxVertexMask): BoxVertexMask {
        return BoxVertexMask(this.mask and other.mask.inv())
    }

    operator fun contains(other: BoxVertexMask): Boolean {
        return this.mask.inv() and other.mask == 0
    }

    fun countBits(digit: Int): Int {
        var sum = 0
        for (i in 1..digit) {
            if (mask and (1 shl i) != 0) sum++
        }
        return sum
    }

    fun toOutlineMask(): BoxOutlineMask {
        var mask = BoxOutlineMask.EMPTY

        if (contains(DOWN)) {
            mask = mask or BoxOutlineMask.DOWN
        }

        if (contains(UP)) {
            mask = mask or BoxOutlineMask.UP
        }

        if (contains(NORTH)) {
            mask = mask or BoxOutlineMask.NORTH
        }

        if (contains(SOUTH)) {
            mask = mask or BoxOutlineMask.SOUTH
        }

        if (contains(WEST)) {
            mask = mask or BoxOutlineMask.WEST
        }

        if (contains(EAST)) {
            mask = mask or BoxOutlineMask.EAST
        }

        return mask
    }

    companion object {
        val EMPTY = BoxVertexMask(0)

        val XN_YN_ZN = BoxVertexMask(1 shl 0) // 0
        val XN_YN_ZP = BoxVertexMask(1 shl 1) // 1
        val XN_YP_ZN = BoxVertexMask(1 shl 2) // 2
        val XN_YP_ZP = BoxVertexMask(1 shl 3) // 3
        val XP_YN_ZN = BoxVertexMask(1 shl 4) // 4
        val XP_YN_ZP = BoxVertexMask(1 shl 5) // 5
        val XP_YP_ZN = BoxVertexMask(1 shl 6) // 6
        val XP_YP_ZP = BoxVertexMask(1 shl 7) // 7

        val DOWN = XN_YN_ZN or XN_YN_ZP or XP_YN_ZN or XP_YN_ZP
        val UP = XN_YP_ZN or XN_YP_ZP or XP_YP_ZN or XP_YP_ZP
        val NORTH = XN_YN_ZN or XN_YP_ZN or XP_YN_ZN or XP_YP_ZN
        val SOUTH = XN_YN_ZP or XN_YP_ZP or XP_YN_ZP or XP_YP_ZP
        val WEST = XN_YN_ZN or XN_YN_ZP or XN_YP_ZN or XN_YP_ZP
        val EAST = XP_YN_ZN or XP_YN_ZP or XP_YP_ZN or XP_YP_ZP
        val ALL = DOWN or UP or NORTH or SOUTH or WEST or EAST

        val DOWN_NORTH = DOWN or NORTH
        val DOWN_SOUTH = DOWN or SOUTH
        val DOWN_WEST = DOWN or WEST
        val DOWN_EAST = DOWN or EAST

        val UP_NORTH = UP or NORTH
        val UP_SOUTH = UP or SOUTH
        val UP_WEST = UP or WEST
        val UP_EAST = UP or EAST

        val NORTH_WEST = NORTH or WEST
        val NORTH_EAST = NORTH or EAST
        val SOUTH_WEST = SOUTH or WEST
        val SOUTH_EAST = SOUTH or EAST

        fun getMaskForSide(side: EnumFacing): BoxVertexMask {
            return when (side) {
                EnumFacing.DOWN -> DOWN
                EnumFacing.UP -> UP
                EnumFacing.NORTH -> NORTH
                EnumFacing.SOUTH -> SOUTH
                EnumFacing.WEST -> WEST
                EnumFacing.EAST -> EAST
            }
        }
    }
}