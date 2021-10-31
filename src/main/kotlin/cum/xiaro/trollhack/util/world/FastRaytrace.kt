package cum.xiaro.trollhack.util.world

import cum.xiaro.trollhack.util.extension.fastFloor
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

fun World.rayTraceVisible(
    start: Vec3d,
    endX: Double,
    endY: Double,
    endZ: Double,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos
): Boolean {
    return !fastRaytrace(start.x, start.y, start.z, endX, endY, endZ, maxAttempt, mutableBlockPos) { pos, blockState ->
        if (blockState.getCollisionBoundingBox(this, pos) != null) {
            FastRayTraceAction.CALC
        } else {
            FastRayTraceAction.SKIP
        }
    }
}

fun World.rayTraceVisible(
    start: Vec3d,
    end: Vec3d,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos
): Boolean {
    return !fastRaytrace(start, end, maxAttempt, mutableBlockPos) { pos, blockState ->
        if (blockState.getCollisionBoundingBox(this, pos) != null) {
            FastRayTraceAction.CALC
        } else {
            FastRayTraceAction.SKIP
        }
    }
}

fun World.fastRaytrace(
    start: Vec3d,
    end: Vec3d,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos
): Boolean {
    return fastRaytrace(start, end, maxAttempt, mutableBlockPos) { _, blockState ->
        if (blockState.block != Blocks.AIR) {
            FastRayTraceAction.CALC
        } else {
            FastRayTraceAction.SKIP
        }
    }
}

fun World.fastRaytrace(
    start: Vec3d,
    end: Vec3d,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos,
    function: (BlockPos, IBlockState) -> FastRayTraceAction
): Boolean = fastRaytrace(start.x, start.y, start.z, end.x, end.y, end.z, maxAttempt, mutableBlockPos, function)

fun World.fastRaytrace(
    start: Vec3d,
    endX: Double,
    endY: Double,
    endZ: Double,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos,
    function: (BlockPos, IBlockState) -> FastRayTraceAction
): Boolean = fastRaytrace(start.x, start.y, start.z, endX, endY, endZ, maxAttempt, mutableBlockPos, function)

fun World.fastRaytrace(
    startX: Double,
    startY: Double,
    startZ: Double,
    endX: Double,
    endY: Double,
    endZ: Double,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos,
    function: (BlockPos, IBlockState) -> FastRayTraceAction
): Boolean {
    var currentX = startX
    var currentY = startY
    var currentZ = startZ

    // Int start position
    var currentBlockX = currentX.fastFloor()
    var currentBlockY = currentY.fastFloor()
    var currentBlockZ = currentZ.fastFloor()

    // Raytrace start block
    mutableBlockPos.setPos(currentBlockX, currentBlockY, currentBlockZ)
    val startBlockState = getBlockState(mutableBlockPos)

    when (function.invoke(mutableBlockPos, startBlockState)) {
        FastRayTraceAction.MISS -> return false
        FastRayTraceAction.CALC -> if (startBlockState.fastRaytrace(this, mutableBlockPos, currentX, currentY, currentZ, endX, endY, endZ)) return true
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
        val blockState = getBlockState(mutableBlockPos)

        when (function.invoke(mutableBlockPos, blockState)) {
            FastRayTraceAction.MISS -> return false
            FastRayTraceAction.CALC -> if (blockState.fastRaytrace(this, mutableBlockPos, currentX, currentY, currentZ, endX, endY, endZ)) return true
            FastRayTraceAction.HIT -> return true
            else -> {

            }
        }
    }

    return false
}

private fun IBlockState.fastRaytrace(
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