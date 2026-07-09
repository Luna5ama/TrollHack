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

package dev.luna5ama.trollhack.utils.math.vectors

import dev.luna5ama.trollhack.utils.math.*
import net.minecraft.world.entity.Entity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

object VectorUtils {
    fun getBlockPosInSphere(entity: Entity, radius: Float): Sequence<BlockPos> {
        return getBlockPosInSphere(entity.x, entity.y, entity.z, radius)
    }

    fun getBlockPosInSphere(pos: Vec3, radius: Float): Sequence<BlockPos> {
        return getBlockPosInSphere(pos.x, pos.y, pos.z, radius)
    }

    fun getBlockPosInSphere(cx: Double, cy: Double, cz: Double, radius: Float): Sequence<BlockPos> {
        val squaredRadius = radius.sq
        val blockPos = BlockPos.MutableBlockPos()

        return sequence {
            for (x in getAxisRange(cx, radius)) {
                for (y in getAxisRange(cy, radius)) {
                    for (z in getAxisRange(cz, radius)) {
                        blockPos.set(x, y, z)
                        if (blockPos.center.distanceTo(cx, cy, cz) > squaredRadius) continue
                        yield(blockPos.immutable())
                    }
                }
            }
        }
    }

    private fun getAxisRange(d1: Double, d2: Float): IntRange {
        return IntRange((d1 - d2).fastFloor(), (d1 + d2).fastCeil())
    }

    fun Vec2f.toViewVec(): Vec3 {
        val yawRad = this.x.toDouble().toRadian()
        val pitchRag = this.y.toDouble().toRadian()
        val yaw = -yawRad - PI_FLOAT
        val pitch = -pitchRag

        val cosYaw = cos(yaw)
        val sinYaw = sin(yaw)
        val cosPitch = -cos(pitch)
        val sinPitch = sin(pitch)

        return Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch)
    }

    fun Vec3i.multiply(multiplier: Int): Vec3i {
        return Vec3i(this.x * multiplier, this.y * multiplier, this.z * multiplier)
    }

    infix operator fun Vec3.times(Vec3: Vec3): Vec3 = Vec3(x * Vec3.x, y * Vec3.y, z * Vec3.z)

    infix operator fun Vec3.times(multiplier: Double): Vec3 =
        Vec3(x * multiplier, y * multiplier, z * multiplier)

    infix operator fun Vec3.times(multiplier: Float): Vec3 =
        Vec3(x * multiplier, y * multiplier, z * multiplier)

    infix operator fun Vec3.plus(Vec3: Vec3): Vec3 = add(Vec3)

    infix operator fun Vec3.minus(Vec3: Vec3): Vec3 = subtract(Vec3)

    fun BlockPos.MutableBlockPos.setAndAdd(set: Vec3i, add: Vec3i): BlockPos.MutableBlockPos {
        return this.set(set.x + add.x, set.y + add.y, set.z + add.z)
    }

    fun BlockPos.MutableBlockPos.setAndAdd(set: Vec3i, x: Int, y: Int, z: Int): BlockPos.MutableBlockPos {
        return this.set(set.x + x, set.y + y, set.z + z)
    }

    fun BlockPos.MutableBlockPos.setAndAdd(set: BlockPos, side: Direction): BlockPos.MutableBlockPos {
        return this.setAndAdd(set, side.unitVec3i)
    }

    fun BlockPos.MutableBlockPos.setAndAdd(set: BlockPos, side: Direction, n: Int): BlockPos.MutableBlockPos {
        val dirVec = side.unitVec3i
        return this.set(set.x + dirVec.x * n, set.y + dirVec.y * n, set.z + dirVec.z * n)
    }
}

