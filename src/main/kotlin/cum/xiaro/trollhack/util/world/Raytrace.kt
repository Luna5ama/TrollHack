package cum.xiaro.trollhack.util.world

import cum.xiaro.trollhack.util.extension.fastFloor
import cum.xiaro.trollhack.util.math.vector.distanceSq
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

fun World.rayTrace(
    start: Vec3d,
    end: Vec3d,
    maxAttempt: Int = 50
): RayTraceResult? {
    return rayTrace(start, end, maxAttempt) { _, blockState ->
        if (blockState.block != Blocks.AIR) {
            RayTraceAction.Calc
        } else {
            RayTraceAction.Skip
        }
    }
}

fun World.rayTrace(
    start: Vec3d,
    end: Vec3d,
    maxAttempt: Int = 50,
    function: (BlockPos, IBlockState) -> RayTraceAction
): RayTraceResult? {
    var currentX = start.x
    var currentY = start.y
    var currentZ = start.z

    // Int start position
    var currentBlockX = currentX.fastFloor()
    var currentBlockY = currentY.fastFloor()
    var currentBlockZ = currentZ.fastFloor()

    // Raytrace start block
    val blockPos = BlockPos.MutableBlockPos(currentBlockX, currentBlockY, currentBlockZ)
    val startBlockState = getBlockState(blockPos)

    val endX = end.x
    val endY = end.y
    val endZ = end.z

    @Suppress("UNNECESSARY_SAFE_CALL")
    when (val action = function.invoke(blockPos, startBlockState)) {
        RayTraceAction.Null -> return null
        RayTraceAction.Calc -> startBlockState.raytrace(this, blockPos, currentX, currentY, currentZ, endX, endY, endZ)?.let { return it }
        is RayTraceAction.Result -> return action.rayTraceResult
    }

    // Int end position
    val endBlockX = endX.fastFloor()
    val endBlockY = endY.fastFloor()
    val endBlockZ = endZ.fastFloor()

    var count = maxAttempt

    while (count-- >= 0) {
        if (currentBlockX == endBlockX && currentBlockY == endBlockY && currentBlockZ == endBlockZ) {
            return null
        }

        var nextX = 999
        var nextY = 999
        var nextZ = 999

        var stepX = 999.0
        var stepY = 999.0
        var stepZ = 999.0
        val diffX = end.x - currentX
        val diffY = end.y - currentY
        val diffZ = end.z - currentZ

        if (endBlockX > currentBlockX) {
            nextX = currentBlockX + 1
            stepX = (nextX - currentX) / diffX
        } else if (endBlockX < currentBlockX) {
            nextX = currentBlockX
            stepX = (nextX - currentX) / diffX
        }

        if (endBlockY > currentBlockY) {
            nextY = currentBlockY + 1
            stepY = (nextY - currentY) / diffY
        } else if (endBlockY < currentBlockY) {
            nextY = currentBlockY
            stepY = (nextY - currentY) / diffY
        }

        if (endBlockZ > currentBlockZ) {
            nextZ = currentBlockZ + 1
            stepZ = (nextZ - currentZ) / diffZ
        } else if (endBlockZ < currentBlockZ) {
            nextZ = currentBlockZ
            stepZ = (nextZ - currentZ) / diffZ
        }

        if (stepX < stepY && stepX < stepZ) {
            currentX = nextX.toDouble()
            currentY += diffY * stepX
            currentZ += diffZ * stepX

            currentBlockX = nextX - (endBlockX - currentBlockX ushr 31)
            currentBlockY = currentY.fastFloor()
            currentBlockZ = currentZ.fastFloor()
        } else if (stepY < stepZ) {
            currentX += diffX * stepY
            currentY = nextY.toDouble()
            currentZ += diffZ * stepY

            currentBlockX = currentX.fastFloor()
            currentBlockY = nextY - (endBlockY - currentBlockY ushr 31)
            currentBlockZ = currentZ.fastFloor()
        } else {
            currentX += diffX * stepZ
            currentY += diffY * stepZ
            currentZ = nextZ.toDouble()

            currentBlockX = currentX.fastFloor()
            currentBlockY = currentY.fastFloor()
            currentBlockZ = nextZ - (endBlockZ - currentBlockZ ushr 31)
        }

        blockPos.setPos(currentBlockX, currentBlockY, currentBlockZ)
        val blockState = getBlockState(blockPos)

        @Suppress("UNNECESSARY_SAFE_CALL")
        when (val action = function.invoke(blockPos, blockState)) {
            RayTraceAction.Null -> return null
            RayTraceAction.Calc -> blockState.raytrace(this, blockPos, currentX, currentY, currentZ, endX, endY, endZ)?.let { return it }
            is RayTraceAction.Result -> return action.rayTraceResult
        }
    }

    return null
}

private fun IBlockState.raytrace(
    world: World,
    blockPos: BlockPos.MutableBlockPos,
    x1: Double,
    y1: Double,
    z1: Double,
    x2: Double,
    y2: Double,
    z2: Double
): RayTraceResult? {
    val x1f = (x1 - blockPos.x).toFloat()
    val y1f = (y1 - blockPos.y).toFloat()
    val z1f = (z1 - blockPos.z).toFloat()

    val box = this.getBoundingBox(world, blockPos)

    val minX = box.minX.toFloat()
    val minY = box.minY.toFloat()
    val minZ = box.minZ.toFloat()
    val maxX = box.maxX.toFloat()
    val maxY = box.maxY.toFloat()
    val maxZ = box.maxZ.toFloat()

    val xDiff = (x2 - blockPos.x).toFloat() - x1f
    val yDiff = (y2 - blockPos.y).toFloat() - y1f
    val zDiff = (z2 - blockPos.z).toFloat() - z1f

    var hitVecX = Float.NaN
    var hitVecY = Float.NaN
    var hitVecZ = Float.NaN
    var side = EnumFacing.WEST
    var none = true

    if (xDiff * xDiff >= 1.0E-7f) {
        val factorMin = (minX - x1f) / xDiff
        if (factorMin in 0.0..1.0) {
            val newY = y1f + yDiff * factorMin
            val newZ = z1f + zDiff * factorMin

            if (newY in minY..maxY && newZ in minZ..maxZ) {
                val newX = x1f + xDiff * factorMin

                if (none || distanceSq(x1f, y1f, z1f, newX, newY, newZ) < distanceSq(x1f, y1f, z1f, hitVecX, hitVecY, hitVecZ)) {
                    hitVecX = newX
                    hitVecY = newY
                    hitVecZ = newZ
                    side = EnumFacing.WEST
                    none = false
                }
            }
        } else {
            val factorMax = (maxX - x1f) / xDiff
            if (factorMax in 0.0..1.0) {
                val newY = y1f + yDiff * factorMax
                val newZ = z1f + zDiff * factorMax

                if (newY in minY..maxY && newZ in minZ..maxZ) {
                    val newX = x1f + xDiff * factorMax

                    if (none || distanceSq(x1f, y1f, z1f, newX, newY, newZ) < distanceSq(x1f, y1f, z1f, hitVecX, hitVecY, hitVecZ)) {
                        hitVecX = newX
                        hitVecY = newY
                        hitVecZ = newZ
                        side = EnumFacing.EAST
                        none = false
                    }
                }
            }
        }
    }

    if (yDiff * yDiff >= 1.0E-7f) {
        val factorMin = (minY - y1f) / yDiff
        if (factorMin in 0.0f..1.0f) {
            val newX = x1f + xDiff * factorMin
            val newZ = z1f + zDiff * factorMin

            if (newX in minX..maxX && newZ in minZ..maxZ) {
                val newY = y1f + yDiff * factorMin

                if (none || distanceSq(x1f, y1f, z1f, newX, newY, newZ) < distanceSq(x1f, y1f, z1f, hitVecX, hitVecY, hitVecZ)) {
                    hitVecX = newX
                    hitVecY = newY
                    hitVecZ = newZ
                    side = EnumFacing.DOWN
                    none = false
                }
            }
        } else {
            val factorMax = (maxY - y1f) / yDiff
            if (factorMax in 0.0f..1.0f) {
                val newX = x1f + xDiff * factorMax
                val newZ = z1f + zDiff * factorMax

                if (newX in minX..maxX && newZ in minZ..maxZ) {
                    val newY = y1f + yDiff * factorMax

                    if (none || distanceSq(x1f, y1f, z1f, newX, newY, newZ) < distanceSq(x1f, y1f, z1f, hitVecX, hitVecY, hitVecZ)) {
                        hitVecX = newX
                        hitVecY = newY
                        hitVecZ = newZ
                        side = EnumFacing.UP
                        none = false
                    }
                }
            }
        }
    }

    if (zDiff * zDiff >= 1.0E-7) {
        val factorMin = (minZ - z1f) / zDiff
        if (factorMin in 0.0f..1.0f) {
            val newX = x1f + xDiff * factorMin
            val newY = y1f + yDiff * factorMin

            if (newX in minX..maxX && newY in minY..maxY) {
                val newZ = z1f + zDiff * factorMin

                if (none || distanceSq(x1f, y1f, z1f, newX, newY, newZ) < distanceSq(x1f, y1f, z1f, hitVecX, hitVecY, hitVecZ)) {
                    hitVecX = newX
                    hitVecY = newY
                    hitVecZ = newZ
                    side = EnumFacing.NORTH
                    none = false
                }
            }
        } else {
            val factorMax = (maxZ - z1f) / zDiff
            if (factorMax in 0.0f..1.0f) {
                val newX = x1f + xDiff * factorMax
                val newY = y1f + yDiff * factorMax

                if (newX in minX..maxX && newY in minY..maxY) {
                    val newZ = z1f + zDiff * factorMax

                    if (none || distanceSq(x1f, y1f, z1f, newX, newY, newZ) < distanceSq(x1f, y1f, z1f, hitVecX, hitVecY, hitVecZ)) {
                        hitVecX = newX
                        hitVecY = newY
                        hitVecZ = newZ
                        side = EnumFacing.SOUTH
                        none = false
                    }
                }
            }
        }
    }

    return if (!none) {
        val hitVec = Vec3d(hitVecX.toDouble() + blockPos.x, hitVecY.toDouble() + blockPos.y, hitVecZ.toDouble() + blockPos.z)
        RayTraceResult(hitVec, side, blockPos.toImmutable())
    } else {
        null
    }
}

sealed class RayTraceAction {
    object Skip : RayTraceAction()
    object Null : RayTraceAction()
    object Calc : RayTraceAction()
    class Result(val rayTraceResult: RayTraceResult) : RayTraceAction()
}