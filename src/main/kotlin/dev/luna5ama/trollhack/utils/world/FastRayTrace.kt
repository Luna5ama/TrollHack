/*
 * Copyright (c) 2021-2022, SagiriXiguajerry. All rights reserved.
 * This repository will be transformed to SuperMic_233.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package dev.luna5ama.trollhack.utils.world

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import dev.luna5ama.trollhack.utils.math.fastFloor
import dev.luna5ama.trollhack.utils.world.FastRayTraceAction.*
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

fun interface FastRayTraceFunction {
    operator fun Level.invoke(
        pos: BlockPos,
        state: BlockState,
        current: Vec3
    ): FastRayTraceAction

    companion object {
        @JvmField
        val DEFAULT = FastRayTraceFunction { pos, state, _ ->
            if (!state.getShape(this, pos).isEmpty) {
                CALC
            } else {
                SKIP
            }
        }
    }
}

fun interface RayTraceFunction {
    operator fun invoke(
        pos: Vec3
    ): FastRayTraceAction
}

private operator fun FastRayTraceFunction.invoke(
    world: Level,
    pos: BlockPos,
    state: BlockState,
    current: Vec3
) = world.invoke(pos, state, current)

fun Level.rayTraceVisible(
    start: Vec3,
    end: Vec3,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()
): Boolean = !fastRayTrace(start, end, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)

fun Level.rayTraceVisible(
    startX: Double,
    startY: Double,
    startZ: Double,
    end: Vec3,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()
): Boolean = !fastRayTrace(startX, startY, startZ, end, maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT)


fun Level.rayTraceVisible(
    start: Vec3,
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

fun Level.rayTraceVisible(
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
fun Level.fastRayTraceCorners(
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
fun Level.fastRayTrace(
    start: Vec3,
    end: Vec3,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(),
    function: FastRayTraceFunction = FastRayTraceFunction.DEFAULT
): Boolean = fastRayTrace(start.x, start.y, start.z, end.x, end.y, end.z, maxAttempt, mutableBlockPos, function)

fun Level.fastRayTrace(
    start: Vec3,
    end: Vec3,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(),
    function: RayTraceFunction
): Boolean = fastRayTrace(start.x, start.y, start.z, end.x, end.y, end.z,
    maxAttempt, mutableBlockPos, FastRayTraceFunction.DEFAULT, function)

/**
 * @return true if hit a block
 */
fun Level.fastRayTrace(
    startX: Double,
    startY: Double,
    startZ: Double,
    end: Vec3,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(),
    function: FastRayTraceFunction = FastRayTraceFunction.DEFAULT
): Boolean = fastRayTrace(startX, startY, startZ, end.x, end.y, end.z, maxAttempt, mutableBlockPos, function)

/**
 * @return true if hit a block
 */
fun Level.fastRayTrace(
    start: Vec3,
    endX: Double,
    endY: Double,
    endZ: Double,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(),
    function: FastRayTraceFunction = FastRayTraceFunction.DEFAULT
): Boolean = fastRayTrace(start.x, start.y, start.z, endX, endY, endZ, maxAttempt, mutableBlockPos, function)

fun Level.rayTrace(
    start: Vec3,
    end: Vec3,
    maxAttempt: Int = 50,
    function: RayTraceFunction
): Boolean = fastRayTrace(start.x, start.y, start.z, end.x, end.y, end.z, maxAttempt, func2 = function)

/**
 * @return true if hit a block
 */
fun Level.fastRayTrace(
    startX: Double,
    startY: Double,
    startZ: Double,
    endX: Double,
    endY: Double,
    endZ: Double,
    maxAttempt: Int = 50,
    mutableBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(),
    function: FastRayTraceFunction = FastRayTraceFunction.DEFAULT,
    func2: RayTraceFunction? = null
): Boolean {
    var currentX = startX
    var currentY = startY
    var currentZ = startZ

    // Int start position
    var currentBlockX = currentX.fastFloor()
    var currentBlockY = currentY.fastFloor()
    var currentBlockZ = currentZ.fastFloor()

    // Ray trace start block
    mutableBlockPos.set(currentBlockX, currentBlockY, currentBlockZ)
    val startBlockState = getBlockState(mutableBlockPos)

    if (func2 != null)
        when (func2.invoke(Vec3(currentX, currentY, currentZ))) {
            HIT -> return true
            MISS -> return false
            else -> {}
        }
    else
        when (function.invoke(this, mutableBlockPos, startBlockState, Vec3(currentX, currentY, currentZ))) {
            MISS -> return false
            CALC -> if (startBlockState.rayTraceBlock(
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
            HIT -> return true
            else -> {}
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

        mutableBlockPos.set(currentBlockX, currentBlockY, currentBlockZ)
        if (isOutsideBuildHeight(mutableBlockPos) || !worldBorder.isWithinBounds(mutableBlockPos)) continue

        val blockState = getBlockState(mutableBlockPos)

        if (func2 != null)
            when (func2.invoke(Vec3(currentX, currentY, currentZ))) {
                HIT -> return true
                MISS -> return false
                else -> {}
            }
        else
            when (function(this, mutableBlockPos, blockState, Vec3(currentX, currentY, currentZ))) {
                MISS -> return false
                CALC -> if (blockState.rayTraceBlock(
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
                HIT -> return true
                else -> {}
            }
    }
    return false
}

private fun BlockState.rayTraceBlock(
    world: Level,
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

    val box = getShape(world, blockPos).takeUnless { it.isEmpty }?.bounds() ?: return false

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

        if (factor in 0.0f..1.0f && y1f + yDiff * factor in minY..maxY
            && z1f + zDiff * factor in minZ..maxZ) {
            return true
        }
    }

    if (yDiff * yDiff >= 1.0E-7f) {
        var factor = (minY - y1f) / yDiff
        if (factor !in 0.0f..1.0f) factor = (maxY - y1f) / yDiff

        if (factor in 0.0f..1.0f && x1f + xDiff * factor in minX..maxX
            && z1f + zDiff * factor in minZ..maxZ) {
            return true
        }
    }

    if (zDiff * zDiff >= 1.0E-7) {
        var factor = (minZ - z1f) / zDiff
        if (factor !in 0.0f..1.0f) factor = (maxZ - z1f) / zDiff

        if (factor in 0.0f..1.0f && x1f + xDiff * factor in minX..maxX
            && y1f + yDiff * factor in minY..maxY) {
            return true
        }
    }

    return false
}

enum class FastRayTraceAction {
    SKIP, MISS, CALC, HIT
}