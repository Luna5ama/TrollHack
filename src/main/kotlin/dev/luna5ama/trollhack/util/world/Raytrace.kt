package dev.luna5ama.trollhack.util.world

import dev.fastmc.common.distanceSq
import dev.fastmc.common.floorToInt
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

fun interface RayTraceFunction {
    operator fun World.invoke(
        pos: BlockPos,
        state: IBlockState
    ): RayTraceAction

    companion object {
        @JvmField
        val DEFAULT = RayTraceFunction { _, blockState ->
            if (blockState.block != Blocks.AIR) {
                RayTraceAction.Calc
            } else {
                RayTraceAction.Skip
            }
        }
    }
}

private operator fun RayTraceFunction.invoke(
    world: World,
    pos: BlockPos,
    state: IBlockState
) = world.invoke(pos, state)

sealed class RayTraceAction {
    object Skip : RayTraceAction()
    object Null : RayTraceAction()
    object Calc : RayTraceAction()
    class Result(val rayTraceResult: RayTraceResult) : RayTraceAction()
}

fun rayTrace(
    world: World,
    start: Vec3d,
    end: Vec3d,
    stopOnLiquid: Boolean,
    ignoreBlockWithoutBoundingBox: Boolean,
    returnLastUncollidableBlock: Boolean,
) = world.rayTrace(
    start,
    end,
    stopOnLiquid,
    ignoreBlockWithoutBoundingBox,
    returnLastUncollidableBlock
)

@JvmName("rayTraceImpl")
fun World.rayTrace(
    start: Vec3d,
    end: Vec3d,
    stopOnLiquid: Boolean,
    ignoreBlockWithoutBoundingBox: Boolean,
    returnLastUncollidableBlock: Boolean
): RayTraceResult? {
    var currentX = start.x
    var currentY = start.y
    var currentZ = start.z

    // Int start position
    var currentBlockX = currentX.floorToInt()
    var currentBlockY = currentY.floorToInt()
    var currentBlockZ = currentZ.floorToInt()

    // Raytrace start block
    val blockPos = BlockPos.MutableBlockPos(currentBlockX, currentBlockY, currentBlockZ)
    val startBlockState = getBlockState(blockPos)

    val endX = end.x
    val endY = end.y
    val endZ = end.z

    if ((!ignoreBlockWithoutBoundingBox
            || startBlockState.getCollisionBoundingBox(this, blockPos) != null)
        && startBlockState.block.canCollideCheck(startBlockState, stopOnLiquid)
    ) {
        startBlockState.rayTrace(this, blockPos, currentX, currentY, currentZ, endX, endY, endZ).let { return it }
    }

    // Int end position
    val endBlockX = endX.floorToInt()
    val endBlockY = endY.floorToInt()
    val endBlockZ = endZ.floorToInt()

    var count = 200

    while (count-- >= 0) {
        if (currentBlockX == endBlockX && currentBlockY == endBlockY && currentBlockZ == endBlockZ) {
            break
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
            currentBlockY = currentY.floorToInt()
            currentBlockZ = currentZ.floorToInt()
        } else if (stepY < stepZ) {
            currentX += diffX * stepY
            currentY = nextY.toDouble()
            currentZ += diffZ * stepY

            currentBlockX = currentX.floorToInt()
            currentBlockY = nextY - (endBlockY - currentBlockY ushr 31)
            currentBlockZ = currentZ.floorToInt()
        } else {
            currentX += diffX * stepZ
            currentY += diffY * stepZ
            currentZ = nextZ.toDouble()

            currentBlockX = currentX.floorToInt()
            currentBlockY = currentY.floorToInt()
            currentBlockZ = nextZ - (endBlockZ - currentBlockZ ushr 31)
        }

        blockPos.setPos(currentBlockX, currentBlockY, currentBlockZ)
        val blockState = getBlockState(blockPos)

        if ((!ignoreBlockWithoutBoundingBox
                || blockState.getCollisionBoundingBox(this, blockPos) != null)
            && blockState.block.canCollideCheck(blockState, stopOnLiquid)
        ) {
            blockState.rayTrace(this, blockPos, currentX, currentY, currentZ, endX, endY, endZ)?.let { return it }
        }
    }

    return if (returnLastUncollidableBlock) {
        val enumFacing = if (currentX == currentX.floorToInt().toDouble()) {
            if (end.x > start.x) EnumFacing.WEST
            else EnumFacing.EAST
        } else if (currentY == currentY.floorToInt().toDouble()) {
            if (end.y > start.y) EnumFacing.DOWN
            else EnumFacing.UP
        } else {
            if (end.z > start.z) EnumFacing.NORTH
            else EnumFacing.SOUTH
        }

        RayTraceResult(
            RayTraceResult.Type.MISS,
            Vec3d(currentX, currentY, currentZ),
            enumFacing,
            blockPos.toImmutable()
        )
    } else {
        null
    }
}

fun World.rayTrace(
    start: Vec3d,
    end: Vec3d,
    maxAttempt: Int = 50,
    function: RayTraceFunction = RayTraceFunction.DEFAULT
): RayTraceResult? {
    var currentX = start.x
    var currentY = start.y
    var currentZ = start.z

    // Int start position
    var currentBlockX = currentX.floorToInt()
    var currentBlockY = currentY.floorToInt()
    var currentBlockZ = currentZ.floorToInt()

    // Raytrace start block
    val blockPos = BlockPos.MutableBlockPos(currentBlockX, currentBlockY, currentBlockZ)
    val startBlockState = getBlockState(blockPos)

    val endX = end.x
    val endY = end.y
    val endZ = end.z

    when (val action = function.invoke(this, blockPos, startBlockState)) {
        RayTraceAction.Null -> return null
        RayTraceAction.Calc -> startBlockState.rayTrace(this, blockPos, currentX, currentY, currentZ, endX, endY, endZ)
            ?.let { return it }
        is RayTraceAction.Result -> return action.rayTraceResult
        else -> {}
    }

    // Int end position
    val endBlockX = endX.floorToInt()
    val endBlockY = endY.floorToInt()
    val endBlockZ = endZ.floorToInt()

    var count = maxAttempt

    while (count-- >= 0) {
        if (currentBlockX == endBlockX && currentBlockY == endBlockY && currentBlockZ == endBlockZ) {
            break
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
            currentBlockY = currentY.floorToInt()
            currentBlockZ = currentZ.floorToInt()
        } else if (stepY < stepZ) {
            currentX += diffX * stepY
            currentY = nextY.toDouble()
            currentZ += diffZ * stepY

            currentBlockX = currentX.floorToInt()
            currentBlockY = nextY - (endBlockY - currentBlockY ushr 31)
            currentBlockZ = currentZ.floorToInt()
        } else {
            currentX += diffX * stepZ
            currentY += diffY * stepZ
            currentZ = nextZ.toDouble()

            currentBlockX = currentX.floorToInt()
            currentBlockY = currentY.floorToInt()
            currentBlockZ = nextZ - (endBlockZ - currentBlockZ ushr 31)
        }

        blockPos.setPos(currentBlockX, currentBlockY, currentBlockZ)
        val blockState = getBlockState(blockPos)

        when (val action = function.invoke(this, blockPos, blockState)) {
            RayTraceAction.Null -> return null
            RayTraceAction.Calc -> blockState.rayTrace(this, blockPos, currentX, currentY, currentZ, endX, endY, endZ)
                ?.let { return it }
            is RayTraceAction.Result -> return action.rayTraceResult
            else -> {}
        }
    }

    return null
}

private fun IBlockState.rayTrace(
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

    val x2f = (x2 - blockPos.x).toFloat()
    val y2f = (y2 - blockPos.y).toFloat()
    val z2f = (z2 - blockPos.z).toFloat()

    val diffX = x2f - x1f
    val diffY = y2f - y1f
    val diffZ = z2f - z1f

    val box = this.getBoundingBox(world, blockPos)

    val minX = box.minX.toFloat()
    val maxX = box.maxX.toFloat()
    val minY = box.minY.toFloat()
    val maxY = box.maxY.toFloat()
    val minZ = box.minZ.toFloat()
    val maxZ = box.maxZ.toFloat()

    var hitX = 0.0f
    var hitY = 0.0f
    var hitZ = 0.0f
    var lastDist = 0.0f
    var hitDirection: EnumFacing? = null

    if (diffX * diffX >= 1.0E-7) {
        val factorMin = (minX - x1f) / diffX
        if (factorMin in 0.0..1.0) {
            val resultX = x1f + diffX * factorMin
            val resultY = y1f + diffY * factorMin
            val resultZ = z1f + diffZ * factorMin

            if (resultY in minY..maxY && resultZ in minZ..maxZ) {
                hitX = resultX
                hitY = resultY
                hitZ = resultZ
                hitDirection = EnumFacing.WEST
                lastDist = distanceSq(x1f, y1f, z1f, resultX, resultY, resultZ)
            }
        }

        val factorMax = (maxX - x1f) / diffX
        if (factorMax in 0.0..1.0) {
            val resultX = x1f + diffX * factorMax
            val resultY = y1f + diffY * factorMax
            val resultZ = z1f + diffZ * factorMax

            if (resultY in minY..maxY && resultZ in minZ..maxZ) {
                val dist = distanceSq(x1f, y1f, z1f, resultX, resultY, resultZ)
                if (hitDirection == null || dist < lastDist) {
                    hitX = resultX
                    hitY = resultY
                    hitZ = resultZ
                    hitDirection = EnumFacing.EAST
                    lastDist = dist
                }
            }
        }
    }


    if (diffY * diffY >= 1.0E-7) {
        val factorMin = (minY - y1f) / diffY
        if (factorMin in 0.0..1.0) {
            val resultX = x1f + diffX * factorMin
            val resultY = y1f + diffY * factorMin
            val resultZ = z1f + diffZ * factorMin

            if (resultX in minX..maxX && resultZ in minZ..maxZ) {
                val dist = distanceSq(x1f, y1f, z1f, resultX, resultY, resultZ)
                if (hitDirection == null || dist < lastDist) {
                    hitX = resultX
                    hitY = resultY
                    hitZ = resultZ
                    hitDirection = EnumFacing.DOWN
                    lastDist = dist
                }
            }
        }

        val factorMax = (maxY - y1f) / diffY
        if (factorMax in 0.0..1.0) {
            val resultX = x1f + diffX * factorMax
            val resultY = y1f + diffY * factorMax
            val resultZ = z1f + diffZ * factorMax

            if (resultX in minX..maxX && resultZ in minZ..maxZ) {
                val dist = distanceSq(x1f, y1f, z1f, resultX, resultY, resultZ)
                if (hitDirection == null || dist < lastDist) {
                    hitX = resultX
                    hitY = resultY
                    hitZ = resultZ
                    hitDirection = EnumFacing.UP
                    lastDist = dist
                }
            }
        }
    }

    if (diffZ * diffZ >= 1.0E-7) {
        val factorMin = (minZ - z1f) / diffZ
        if (factorMin in 0.0..1.0) {
            val resultX = x1f + diffX * factorMin
            val resultY = y1f + diffY * factorMin
            val resultZ = z1f + diffZ * factorMin

            if (resultX in minX..maxX && resultY in minY..maxY) {
                val dist = distanceSq(x1f, y1f, z1f, resultX, resultY, resultZ)
                if (hitDirection == null || dist < lastDist) {
                    hitX = resultX
                    hitY = resultY
                    hitZ = resultZ
                    hitDirection = EnumFacing.NORTH
                    lastDist = dist
                }
            }
        }

        val factorMax = (maxZ - z1f) / diffZ
        if (factorMax in 0.0..1.0) {
            val resultX = x1f + diffX * factorMax
            val resultY = y1f + diffY * factorMax
            val resultZ = z1f + diffZ * factorMax

            if (resultX in minX..maxX && resultY in minY..maxY) {
                val dist = distanceSq(x1f, y1f, z1f, resultX, resultY, resultZ)
                if (hitDirection == null || dist < lastDist) {
                    hitX = resultX
                    hitY = resultY
                    hitZ = resultZ
                    hitDirection = EnumFacing.SOUTH
                }
            }
        }
    }

    return if (hitDirection != null) {
        RayTraceResult(
            Vec3d(
                blockPos.x.toDouble() + hitX,
                blockPos.y.toDouble() + hitY,
                blockPos.z.toDouble() + hitZ
            ),
            hitDirection,
            blockPos
        )
    } else {
        null
    }
}


