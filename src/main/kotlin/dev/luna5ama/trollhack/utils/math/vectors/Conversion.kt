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
@file:Suppress("nothing_to_inline")

package dev.luna5ama.trollhack.utils.math.vectors

import dev.luna5ama.trollhack.utils.math.fastFloor
import dev.luna5ama.trollhack.utils.math.fastFloorToDouble
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec3

inline val Vec3.floored get() = Vec3(x.fastFloorToDouble(), y.fastFloorToDouble(), z.fastFloorToDouble())
inline val Vec3.flooredWithinXZ get() = Vec3(x.fastFloorToDouble(), y, z.fastFloorToDouble())

inline fun Vec3.toBlockPos(xOffset: Int, yOffset: Int, zOffset: Int): BlockPos {
    return BlockPos(x.fastFloor() + xOffset, y.fastFloor() + yOffset, z.fastFloor() + zOffset)
}

inline fun Vec3.toBlockPos(): BlockPos {
    return BlockPos(x.fastFloor(), y.fastFloor(), z.fastFloor())
}

inline fun Vec3i.toVec3Center(): Vec3 {
    return this.toVec3(0.5, 0.5, 0.5)
}

inline fun Vec3i.toVec3Center(xOffset: Double, yOffset: Double, zOffset: Double): Vec3 {
    return this.toVec3(0.5 + xOffset, 0.5 + yOffset, 0.5 + zOffset)
}

inline fun Vec3i.toVec3(): Vec3 {
    return Vec3(x.toDouble(), y.toDouble(), z.toDouble())
}

inline fun Vec3i.toVec3(offSet: Vec3): Vec3 {
    return this.toVec3(offSet.x, offSet.y, offSet.z)
}

inline fun Vec3i.toVec3(xOffset: Double, yOffset: Double, zOffset: Double): Vec3 {
    return Vec3(x + xOffset, y + yOffset, z + zOffset)
}