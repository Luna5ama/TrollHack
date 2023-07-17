package dev.luna5ama.trollhack.util.combat

import dev.fastmc.common.floorToInt
import dev.luna5ama.trollhack.graphics.mask.EnumFacingMask
import dev.luna5ama.trollhack.module.modules.combat.CombatSetting
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.util.math.Vec3d
import kotlin.math.max

class ExposureSample(
    val width: Float,
    val height: Float
) {
    private val array: Array<Point>
    val maxY: Double

    init {
        val gridMultiplierXZ = 1.0 / (width * 2.0 + 1.0)
        val gridMultiplierY = 1.0 / (height * 2.0 + 1.0)

        val gridXZ = width * gridMultiplierXZ
        val gridY = height * gridMultiplierY

        val sizeXZ = (1.0 / gridMultiplierXZ).floorToInt()
        val sizeY = (1.0 / gridMultiplierY).floorToInt()
        val xzOffset = (1.0 - gridMultiplierXZ * (sizeXZ)) / 2.0

        val list = ArrayList<Point>()
        var maxYTemp = 0.0

        for (yIndex in 0..sizeY) {
            for (xIndex in 0..sizeXZ) {
                for (zIndex in 0..sizeXZ) {
                    if (!CombatSetting.horizontalCenterSampling) {
                        if (xIndex != 0 && xIndex != sizeXZ && zIndex != 0 && zIndex != sizeXZ) {
                            continue
                        }
                    }

                    if (!CombatSetting.verticalCenterSampling) {
                        if (yIndex != 0 && yIndex != sizeY && (xIndex != 0 && xIndex != sizeXZ || zIndex != 0 && zIndex != sizeXZ)) {
                            continue
                        }
                    }

                    var mask = 0x00

                    if (yIndex == 0) {
                        mask = EnumFacingMask.DOWN
                    } else if (yIndex == sizeY) {
                        mask = EnumFacingMask.UP
                    }

                    if (xIndex == 0) {
                        mask = mask or EnumFacingMask.WEST
                    } else if (xIndex == sizeXZ) {
                        mask = mask or EnumFacingMask.EAST
                    }

                    if (zIndex == 0) {
                        mask = mask or EnumFacingMask.NORTH
                    } else if (zIndex == sizeXZ) {
                        mask = mask or EnumFacingMask.SOUTH
                    }

                    val x = gridXZ * xIndex + xzOffset
                    val y = gridY * yIndex
                    val z = gridXZ * zIndex + xzOffset

                    maxYTemp = max(y, maxYTemp)
                    list.add(Point(x, y, z, mask))
                }
            }
        }

        maxY = maxYTemp
        array = list.toTypedArray()
    }

    fun offset(x: Double, y: Double, z: Double): Array<Vec3d> {
        return Array(array.size) {
            val point = array[it]
            Vec3d(point.x + x, point.y + y, point.z + z)
        }
    }

    fun getMask(index: Int): Int {
        return array[index].mask
    }

    override fun equals(other: Any?): Boolean {
        return this === other
            || (other is ExposureSample
            && width == other.width
            && height == other.height)
    }

    override fun hashCode(): Int {
        return 31 * width.hashCode() + height.hashCode()
    }

    private class Point(
        val x: Double,
        val y: Double,
        val z: Double,
        val mask: Int
    )

    companion object {
        private val samplePointsCache = Long2ObjectOpenHashMap<ExposureSample>()

        fun getExposureSample(width: Float, height: Float): ExposureSample {
            val key = (width.toRawBits().toLong() shl 32) and height.toRawBits().toLong()
            var result = samplePointsCache.get(key)

            if (result == null) {
                result = ExposureSample(width, height)
                synchronized(samplePointsCache) {
                    samplePointsCache.put(key, result)
                }
            }

            return result
        }

        fun resetSamplePoints() {
            samplePointsCache.clear()
        }
    }
}