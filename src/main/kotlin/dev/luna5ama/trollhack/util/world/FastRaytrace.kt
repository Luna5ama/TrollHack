package dev.luna5ama.trollhack.util.world

import dev.fastmc.common.floorToInt
import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

fun interface FastRayTraceFunction {
    operator fun World.invoke(
        pos: BlockPos,
        state: IBlockState
    ): FastRayTraceAction

    companion object {
        @JvmField
        val DEFAULT = FastRayTraceFunction { pos, state ->
            if (state.getCollisionBoundingBox(this, pos) != null) {
                FastRayTraceAction.CALC
            } else {
                FastRayTraceAction.SKIP
            }
        }
    }
}

private operator fun FastRayTraceFunction.invoke(
    world: World,
    pos: BlockPos,
    state: IBlockState
) = world.invoke(pos, state)

fun World.rayTraceVisible(
    start: Vec3d,
    end: Vec3d,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()
): Boolean = !fastRayTrace(start, end, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)

fun World.rayTraceVisible(
    startX: Double,
    startY: Double,
    startZ: Double,
    end: Vec3d,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()
): Boolean = !fastRayTrace(startX, startY, startZ, end, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)


fun World.rayTraceVisible(
    start: Vec3d,
    endX: Double,
    endY: Double,
    endZ: Double,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()
): Boolean = !fastRayTrace(
    start.x,
    start.y,
    start.z,
    endX,
    endY,
    endZ,
    maxAttempt,
    mutableBlockPos,
    FastRayTraceFunction.DEFAULT
)

fun World.rayTraceVisible(
    startX: Double,
    startY: Double,
    startZ: Double,
    endX: Double,
    endY: Double,
    endZ: Double,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(),
): Boolean = !fastRayTrace(
    startX,
    startY,
    startZ,
    endX,
    endY,
    endZ,
    maxAttempt,
    mutableBlockPos,
    FastRayTraceFunction.DEFAULT
)

/**
 * @return number of rays from corners that hit a block
 */
fun World.fastRayTraceCorners(
    x: Double,
    y: Double,
    z: Double,
    blockX: Int,
    blockY: Int,
    blockZ: Int,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()
): Int {
    val minX = blockX + 0.05
    val minY = blockY + 0.05
    val minZ = blockZ + 0.05

    val maxX = blockX + 0.95
    val maxY = blockY + 0.95
    val maxZ = blockZ + 0.95

    var count = 0

    if (fastRayTrace(x, y, z, minX, minY, minZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)) count++
    if (fastRayTrace(x, y, z, maxX, minY, minZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)) count++
    if (fastRayTrace(x, y, z, minX, minY, maxZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)) count++
    if (fastRayTrace(x, y, z, maxX, minY, maxZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)) count++
    if (fastRayTrace(x, y, z, minX, maxY, minZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)) count++
    if (fastRayTrace(x, y, z, maxX, maxY, minZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)) count++
    if (fastRayTrace(x, y, z, minX, maxY, maxZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)) count++
    if (fastRayTrace(x, y, z, maxX, maxY, maxZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)) count++

    return count
}

/**
 * @return true if hit a block
 */
fun World.fastRayTrace(
    start: Vec3d,
    end: Vec3d,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(),
    function: FastRayTraceFunction = FastRayTraceFunction.DEFAULT
): Boolean = fastRayTrace(start.x, start.y, start.z, end.x, end.y, end.z, maxAttempt, mutableBlockPos, function)

/**
 * @return true if hit a block
 */
fun World.fastRayTrace(
    startX: Double,
    startY: Double,
    startZ: Double,
    end: Vec3d,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(),
    function: FastRayTraceFunction = FastRayTraceFunction.DEFAULT
): Boolean = fastRayTrace(startX, startY, startZ, end.x, end.y, end.z, maxAttempt, mutableBlockPos, function)

/**
 * @return true if hit a block
 */
fun World.fastRayTrace(
    start: Vec3d,
    endX: Double,
    endY: Double,
    endZ: Double,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(),
    function: FastRayTraceFunction = FastRayTraceFunction.DEFAULT
): Boolean = fastRayTrace(start.x, start.y, start.z, endX, endY, endZ, maxAttempt, mutableBlockPos, function)

/**
 * @return true if hit a block
 */
fun World.fastRayTrace(
    startX: Double,
    startY: Double,
    startZ: Double,
    endX: Double,
    endY: Double,
    endZ: Double,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(),
    function: FastRayTraceFunction = FastRayTraceFunction.DEFAULT
): Boolean {
    var currentX = startX
    var currentY = startY
    var currentZ = startZ

    // Int start position
    var currentBlockX = currentX.floorToInt()
    var currentBlockY = currentY.floorToInt()
    var currentBlockZ = currentZ.floorToInt()

    // Ray trace start block
    mutableBlockPos.setPos(currentBlockX, currentBlockY, currentBlockZ)
    val startBlockState = getBlockState(mutableBlockPos)

    when (function(this, mutableBlockPos, startBlockState)) {
        FastRayTraceAction.MISS -> return false
        FastRayTraceAction.CALC -> if (startBlockState.rayTraceBlock(
                this,
                mutableBlockPos,
                currentX,
                currentY,
                currentZ,
                endX,
                endY,
                endZ
            )
        ) return true
        FastRayTraceAction.HIT -> return true
        else -> {

        }
    }

    // Int end position
    val endBlockX = endX.floorToInt()
    val endBlockY = endY.floorToInt()
    val endBlockZ = endZ.floorToInt()

    var count = maxAttempt

    while (count-- >= 0) {
        if (currentBlockX == endBlockX && currentBlockY == endBlockY && currentBlockZ == endBlockZ) {
            return false
        }

        var nextX = 999
        var nextY = 999
        var nextZ = 999

        var stepX = 999.0
        var stepY = 999.0
        var stepZ = 999.0
        val diffX = endX - currentX
        val diffY = endY - currentY
        val diffZ = endZ - currentZ

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

        mutableBlockPos.setPos(currentBlockX, currentBlockY, currentBlockZ)
        if (isOutsideBuildHeight(mutableBlockPos) || !worldBorder.contains(mutableBlockPos)) continue

        val blockState = getBlockState(mutableBlockPos)

        when (function(this, mutableBlockPos, blockState)) {
            FastRayTraceAction.MISS -> return false
            FastRayTraceAction.CALC -> if (blockState.rayTraceBlock(
                    this,
                    mutableBlockPos,
                    currentX,
                    currentY,
                    currentZ,
                    endX,
                    endY,
                    endZ
                )
            ) return true
            FastRayTraceAction.HIT -> return true
            else -> {

            }
        }
    }

    return false
}

private fun IBlockState.rayTraceBlock(
    world: World,
    blockPos: BlockPos.MutableBlockPos,
    x1: Double,
    y1: Double,
    z1: Double,
    x2: Double,
    y2: Double,
    z2: Double
): Boolean {
    val x1f = (x1 - blockPos.x).toFloat()
    val y1f = (y1 - blockPos.y).toFloat()
    val z1f = (z1 - blockPos.z).toFloat()

    val x2f = (x2 - blockPos.x).toFloat()
    val y2f = (y2 - blockPos.y).toFloat()
    val z2f = (z2 - blockPos.z).toFloat()

    val box = this.getBoundingBox(world, blockPos)

    val minX = box.minX.toFloat()
    val minY = box.minY.toFloat()
    val minZ = box.minZ.toFloat()
    val maxX = box.maxX.toFloat()
    val maxY = box.maxY.toFloat()
    val maxZ = box.maxZ.toFloat()

    val xDiff = x2f - x1f
    val yDiff = y2f - y1f
    val zDiff = z2f - z1f

    if (xDiff * xDiff >= 1.0E-7f) {
        var factor = (minX - x1f) / xDiff
        if (factor !in 0.0f..1.0f) factor = (maxX - x1f) / xDiff

        if (factor in 0.0f..1.0f && y1f + yDiff * factor in minY..maxY && z1f + zDiff * factor in minZ..maxZ) {
            return true
        }
    }

    if (yDiff * yDiff >= 1.0E-7f) {
        var factor = (minY - y1f) / yDiff
        if (factor !in 0.0f..1.0f) factor = (maxY - y1f) / yDiff

        if (factor in 0.0f..1.0f && x1f + xDiff * factor in minX..maxX && z1f + zDiff * factor in minZ..maxZ) {
            return true
        }
    }

    if (zDiff * zDiff >= 1.0E-7) {
        var factor = (minZ - z1f) / zDiff
        if (factor !in 0.0f..1.0f) factor = (maxZ - z1f) / zDiff

        if (factor in 0.0f..1.0f && x1f + xDiff * factor in minX..maxX && y1f + yDiff * factor in minY..maxY) {
            return true
        }
    }

    return false
}

enum class FastRayTraceAction {
    SKIP, MISS, CALC, HIT
}