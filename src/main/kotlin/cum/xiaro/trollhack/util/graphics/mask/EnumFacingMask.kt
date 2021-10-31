package cum.xiaro.trollhack.util.graphics.mask

import net.minecraft.util.EnumFacing

object EnumFacingMask {
    const val DOWN = 1 shl 0
    const val UP = 1 shl 1
    const val NORTH = 1 shl 2
    const val SOUTH = 1 shl 3
    const val WEST = 1 shl 4
    const val EAST = 1 shl 5
    const val ALL = DOWN or UP or NORTH or SOUTH or WEST or EAST

    fun getMaskForSide(side: EnumFacing): Int {
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