package dev.luna5ama.trollhack.util.world

import dev.luna5ama.trollhack.util.extension.fastFloor
import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import kotlin.math.max
import kotlin.math.min

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
): Boolean = !fastRayTrace(start.x, start.y, start.z, endX, endY, endZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)

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

fun World.rayTraceCornersVisible(
    x: Double,
    y: Double,
    z: Double,
    blockX: Int,
    blockY: Int,
    blockZ: Int,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()
) = !fastRayTraceCorners(x, y, z, blockX, blockY, blockZ, maxAttempt, mutableBlockPos)

fun World.fastRayTraceCorners(
    x: Double,
    y: Double,
    z: Double,
    blockX: Int,
    blockY: Int,
    blockZ: Int,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()
): Boolean {
    val minX = blockX + 0.05
    val minY = blockY + 0.05
    val minZ = blockZ + 0.05

    val maxX = minX + 0.95
    val maxY = minY + 0.95
    val maxZ = minZ + 0.95

    return fastRayTrace(x, y, z, minX, minY, minZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)
        || fastRayTrace(x, y, z, maxX, minY, minZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)
        || fastRayTrace(x, y, z, minX, minY, maxZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)
        || fastRayTrace(x, y, z, maxX, minY, maxZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)
        || fastRayTrace(x, y, z, minX, maxY, minZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)
        || fastRayTrace(x, y, z, maxX, maxY, minZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)
        || fastRayTrace(x, y, z, minX, maxY, maxZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)
        || fastRayTrace(x, y, z, maxX, maxY, maxZ, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)
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
    var currentBlockX = currentX.fastFloor()
    var currentBlockY = currentY.fastFloor()
    var currentBlockZ = currentZ.fastFloor()

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
    val endBlockX = endX.fastFloor()
    val endBlockY = endY.fastFloor()
    val endBlockZ = endZ.fastFloor()

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
    val box = this.getBoundingBox(world, blockPos)
    return (box.minX + blockPos.x) < max(x1, x2) && (box.maxX + blockPos.x) > min(x1, x1)
        && (box.minY + blockPos.y) < max(y1, y2) && (box.maxY + blockPos.y) > min(y1, y2)
        && (box.minZ + blockPos.z) < max(z1, z2) && (box.maxZ + blockPos.z) > min(z1, z2)
}

enum class FastRayTraceAction {
    SKIP, MISS, CALC, HIT
}