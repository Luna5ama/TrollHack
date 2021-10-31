package cum.xiaro.trollhack.util.graphics.mask

import net.minecraft.util.EnumFacing

@JvmInline
value class SideMask private constructor(val mask: Int) {
    val isEmpty get() = mask == 0

    operator fun plus(other: SideMask): SideMask {
        return SideMask(this.mask or other.mask)
    }

    operator fun minus(other: SideMask): SideMask {
        return SideMask(this.mask and other.mask.inv())
    }

    operator fun contains(other: SideMask): Boolean {
        return this.mask.inv() and other.mask == 0
    }

    fun toVertexMask(): BoxVertexMask {
        var mask = BoxVertexMask.EMPTY

        if (contains(DOWN)) {
            mask += BoxVertexMask.DOWN
        }

        if (contains(UP)) {
            mask += BoxVertexMask.UP
        }

        if (contains(NORTH)) {
            mask += BoxVertexMask.NORTH
        }

        if (contains(SOUTH)) {
            mask += BoxVertexMask.SOUTH
        }

        if (contains(WEST)) {
            mask += BoxVertexMask.WEST
        }

        if (contains(EAST)) {
            mask += BoxVertexMask.EAST
        }

        return mask
    }

    fun toOutlineMask(): BoxOutlineMask {
        var mask = BoxOutlineMask.EMPTY

        if (contains(DOWN)) {
            mask += BoxOutlineMask.DOWN
        }

        if (contains(UP)) {
            mask += BoxOutlineMask.UP
        }

        if (contains(NORTH)) {
            mask += BoxOutlineMask.NORTH
        }

        if (contains(SOUTH)) {
            mask += BoxOutlineMask.SOUTH
        }

        if (contains(WEST)) {
            mask += BoxOutlineMask.WEST
        }

        if (contains(EAST)) {
            mask += BoxOutlineMask.EAST
        }

        return mask
    }

    fun toOutlineMaskInv(): BoxOutlineMask {
        var mask = BoxOutlineMask.ALL

        if (!contains(DOWN)) {
            mask -= BoxOutlineMask.DOWN
        }

        if (!contains(UP)) {
            mask -= BoxOutlineMask.UP
        }

        if (!contains(NORTH)) {
            mask -= BoxOutlineMask.NORTH
        }

        if (!contains(SOUTH)) {
            mask -= BoxOutlineMask.SOUTH
        }

        if (!contains(WEST)) {
            mask -= BoxOutlineMask.WEST
        }

        if (!contains(EAST)) {
            mask -= BoxOutlineMask.EAST
        }

        return mask
    }

    companion object {
        val EMPTY = SideMask(0)

        val DOWN = SideMask(1 shl 0)
        val UP = SideMask(1 shl 1)
        val NORTH = SideMask(1 shl 2)
        val SOUTH = SideMask(1 shl 3)
        val WEST = SideMask(1 shl 4)
        val EAST = SideMask(1 shl 5)
        val ALL = SideMask(0x3F)

        fun EnumFacing.toMask(): SideMask {
            return when (this) {
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