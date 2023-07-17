package dev.luna5ama.trollhack.util.math.vector

import dev.fastmc.common.floorToInt
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

fun Vec3d.toBlockPos(xOffset: Int, yOffset: Int, zOffset: Int): BlockPos {
    return BlockPos(x.floorToInt() + xOffset, y.floorToInt() + yOffset, z.floorToInt() + zOffset)
}

fun Vec3d.toBlockPos(): BlockPos {
    return BlockPos(x.floorToInt(), y.floorToInt(), z.floorToInt())
}

fun Vec3i.toVec3dCenter(): Vec3d {
    return this.toVec3d(0.5, 0.5, 0.5)
}

fun Vec3i.toVec3dCenter(xOffset: Double, yOffset: Double, zOffset: Double): Vec3d {
    return this.toVec3d(0.5 + xOffset, 0.5 + yOffset, 0.5 + zOffset)
}

fun Vec3i.toVec3d(): Vec3d {
    return Vec3d(this)
}

fun Vec3i.toVec3d(offSet: Vec3d): Vec3d {
    return this.toVec3d(offSet.x, offSet.y, offSet.z)
}

fun Vec3i.toVec3d(offSet: Vec3f): Vec3d {
    return this.toVec3d(offSet.x.toDouble(), offSet.y.toDouble(), offSet.z.toDouble())
}

fun Vec3i.toVec3d(xOffset: Double, yOffset: Double, zOffset: Double): Vec3d {
    return Vec3d(x + xOffset, y + yOffset, z + zOffset)
}

val NUM_X_BITS = 1 + MathHelper.log2(MathHelper.smallestEncompassingPowerOfTwo(30000000))
val NUM_Z_BITS = NUM_X_BITS
val NUM_Y_BITS = 64 - NUM_X_BITS - NUM_Z_BITS
val Y_SHIFT = 0 + NUM_Z_BITS
val X_SHIFT = Y_SHIFT + NUM_Y_BITS
val X_MASK = (1L shl NUM_X_BITS) - 1L
val Y_MASK = (1L shl NUM_Y_BITS) - 1L
val Z_MASK = (1L shl NUM_Z_BITS) - 1L

fun toLong(x: Int, y: Int, z: Int): Long {
    return x.toLong() and X_MASK shl X_SHIFT or
        (y.toLong() and Y_MASK shl Y_SHIFT) or
        (z.toLong() and Z_MASK)
}