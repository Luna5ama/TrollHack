package dev.luna5ama.trollhack.utils.world.explosion.advanced

import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.extension.checkBlockCollision
import dev.luna5ama.trollhack.utils.math.floorToInt
import dev.luna5ama.trollhack.utils.math.vectors.distanceSqTo
import dev.luna5ama.trollhack.utils.math.vectors.distanceTo
import dev.luna5ama.trollhack.utils.world.*
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.entity.LivingEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import net.minecraft.world.Difficulty
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import kotlin.math.floor
import kotlin.math.min

class CalcContext(
    private val event: NonNullContext,
    val entity: LivingEntity,
    val predictPos: Vec3
) {
    val currentPos: Vec3 = entity.position()
    val currentBox = getBoundingBox(entity, currentPos)
    val predictBox = getBoundingBox(entity, predictPos)
    val clipped = event.world.checkBlockCollision(currentBox)

    private val predicting = currentPos.distanceSqTo(predictPos) > 0.01
    private val difficulty = event.world.difficulty
    private val reduction = DamageReduction(entity)

    private val exposureSample = ExposureSample.getExposureSample(entity.bbWidth, entity.bbHeight)
    private val samplePoints = exposureSample.offset(currentBox.minX, currentBox.minY, currentBox.minZ)
    private val samplePointsPredict = exposureSample.offset(predictBox.minX, predictBox.minY, predictBox.minZ)

    fun checkColliding(pos: Vec3): Boolean {
        val box = AABB(
            pos.x - 0.499, pos.y, pos.z - 0.499,
            pos.x + 0.499, pos.y + 2.0, pos.z + 0.499
        )

        return !box.intersects(currentBox)
            && (!predicting
            || box.clip(currentPos, predictPos).isEmpty
            && !box.intersects(predictBox))
    }

    fun calcDamage(
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        predict: Boolean,
        mutableBlockPos: BlockPos.MutableBlockPos,
    ) = calcDamage(crystalX, crystalY, crystalZ, predict, 6.0f, mutableBlockPos)

    fun calcDamage(
        pos: Vec3,
        predict: Boolean,
        mutableBlockPos: BlockPos.MutableBlockPos,
    ) = calcDamage(pos, predict, 6.0f, mutableBlockPos)

    fun calcDamage(
        pos: Vec3,
        predict: Boolean,
        size: Float,
        mutableBlockPos: BlockPos.MutableBlockPos
    ) = calcDamage(pos.x, pos.y, pos.z, predict, size, mutableBlockPos)

    fun calcDamage(
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        predict: Boolean,
        size: Float,
        mutableBlockPos: BlockPos.MutableBlockPos
    ) = calcDamage(crystalX, crystalY, crystalZ, predict, size, mutableBlockPos) { _, blockState, _ ->
        if (blockState.block != Blocks.AIR && CrystalUtils.isResistant(blockState)) {
            FastRayTraceAction.CALC
        } else {
            FastRayTraceAction.SKIP
        }
    }

    fun calcDamage(
        pos: Vec3,
        predict: Boolean,
        mutableBlockPos: BlockPos.MutableBlockPos,
        function: FastRayTraceFunction
    ) = calcDamage(pos, predict, 6.0f, mutableBlockPos, function)

    fun calcDamage(
        pos: Vec3,
        predict: Boolean,
        size: Float,
        mutableBlockPos: BlockPos.MutableBlockPos,
        function: FastRayTraceFunction
    ) = calcDamage(pos.x, pos.y, pos.z, predict, size, mutableBlockPos, function)

    fun calcDamage(
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        predict: Boolean,
        size: Float,
        mutableBlockPos: BlockPos.MutableBlockPos,
        function: FastRayTraceFunction
    ): Float {
        if (difficulty == Difficulty.PEACEFUL) return 0.0f

        event {
            val entityPos = if (predict) predictPos else currentPos
            var damage =
                if (crystalY - entityPos.y > exposureSample.maxY
                    && CrystalUtils.isResistant(
                        world.getBlockState(
                            mutableBlockPos.set(
                                crystalX.floorToInt(),
                                crystalY.floorToInt() - 1,
                                crystalZ.floorToInt()
                            )
                        )
                    )
                ) {
                    1.0f
                } else {
                    calcRawDamage(crystalX, crystalY, crystalZ, size, predict, mutableBlockPos, function)
                }

            damage = calcDifficultyDamage(damage)
            return reduction.calcDamage(damage, true)
        }
    }

    private fun calcDifficultyDamage(damage: Float) =
        if (entity is Player) {
            when (difficulty) {
                Difficulty.EASY -> {
                    min(damage * 0.5f + 1.0f, damage)
                }
                Difficulty.HARD -> {
                    damage * 1.5f
                }
                else -> {
                    damage
                }
            }
        } else {
            damage
        }

    private fun NonNullContext.calcRawDamage(
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        size: Float,
        predict: Boolean,
        mutableBlockPos: BlockPos.MutableBlockPos,
        function: FastRayTraceFunction
    ): Float {
        val entityPos = if (predict) predictPos else currentPos
        val doubleSize = size * 2.0f
        val scaledDist = entityPos.distanceTo(crystalX, crystalY, crystalZ).toFloat() / doubleSize
        if (scaledDist > 1.0f) return 0.0f

        val factor =
            (1.0f - scaledDist) * getExposureAmount(crystalX, crystalY, crystalZ, predict, mutableBlockPos, function)
        return floor((factor * factor + factor) * doubleSize * 3.5f + 1.0f)
    }

    private fun NonNullContext.getExposureAmount(
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        predict: Boolean,
        mutableBlockPos: BlockPos.MutableBlockPos,
        function: FastRayTraceFunction
    ): Float {
        val box = if (predict) predictBox else currentBox
        if (!clipped && box.isInside(crystalX, crystalY, crystalZ)) return 1.0f

        val array = if (predict) samplePointsPredict else samplePoints
        return if (!ClientSettings.backSideSampling) {
            countSamplePointsOptimized(array, box, crystalX, crystalY, crystalZ, mutableBlockPos, function)
        } else {
            countSamplePoints(array, crystalX, crystalY, crystalZ, mutableBlockPos, function)
        }
    }

    private fun NonNullContext.countSamplePoints(
        samplePoints: Array<Vec3>,
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        blockPos: BlockPos.MutableBlockPos,
        function: FastRayTraceFunction
    ): Float {
        var count = 0

        for (i in samplePoints.indices) {
            val samplePoint = samplePoints[i]
            if (!world.fastRayTrace(samplePoint, crystalX, crystalY, crystalZ, 20, blockPos, function)) {
                count++
            }
        }

        return count.toFloat() / samplePoints.size
    }

    private fun NonNullContext.countSamplePointsOptimized(
        samplePoints: Array<Vec3>,
        box: AABB,
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        mutableBlockPos: BlockPos.MutableBlockPos,
        function: FastRayTraceFunction
    ): Float {
        var count = 0
        var total = 0

        val sideMask = getSideMask(box, crystalX, crystalY, crystalZ)
        for (i in samplePoints.indices) {
            val pointMask = exposureSample.getMask(i)
            if (sideMask and pointMask == 0x00) {
                continue
            }

            total++
            val samplePoint = samplePoints[i]
            if (!world.fastRayTrace(samplePoint, crystalX, crystalY, crystalZ, 20, mutableBlockPos, function)) {
                count++
            }
        }

        return count.toFloat() / total.toFloat()
    }

    private fun getSideMask(
        box: AABB,
        posX: Double,
        posY: Double,
        posZ: Double
    ): Int {
        var mask = 0x00

        if (posX < box.minX) {
            mask = DirectionMask.WEST
        } else if (posX > box.maxX) {
            mask = DirectionMask.EAST
        }

        if (posY < box.minY) {
            mask = mask or DirectionMask.DOWN
        } else if (posY > box.maxY) {
            mask = mask or DirectionMask.UP
        }

        if (posZ < box.minZ) {
            mask = mask or DirectionMask.NORTH
        } else if (posZ > box.maxZ) {
            mask = mask or DirectionMask.SOUTH
        }

        return mask
    }

    private fun AABB.isInside(
        x: Double,
        y: Double,
        z: Double
    ): Boolean {
        return x >= this.minX && x <= this.maxX
            && y >= this.minY && y <= this.maxY
            && z >= this.minZ && z <= this.maxZ
    }

    private fun getBoundingBox(entity: LivingEntity, pos: Vec3): AABB {
        val halfWidth = min(entity.bbWidth.toDouble(), 2.0) / 2.0
        val height = min(entity.bbHeight.toDouble(), 3.0)

        return AABB(
            pos.x - halfWidth, pos.y, pos.z - halfWidth,
            pos.x + halfWidth, pos.y + height, pos.z + halfWidth
        )
    }

    override fun toString(): String {
        return "CalcContext(predictPos=$predictPos, entity=$entity, currentPos=$currentPos, currentBox=$currentBox, " +
                "predictBox=$predictBox, clipped=$clipped, predicting=$predicting, difficulty=$difficulty, " +
                "reduction=$reduction, exposureSample=$exposureSample, samplePoints=${samplePoints.contentToString()}, " +
                "samplePointsPredict=${samplePointsPredict.contentToString()})"
    }
}