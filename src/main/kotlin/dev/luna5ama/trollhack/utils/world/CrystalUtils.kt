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

import dev.luna5ama.trollhack.manager.managers.RotationManager
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.extension.state
import dev.luna5ama.trollhack.utils.math.floorToInt
import dev.luna5ama.trollhack.utils.math.RotationUtils
import dev.luna5ama.trollhack.utils.math.toRadian
import dev.luna5ama.trollhack.utils.math.vectors.VectorUtils.setAndAdd
import dev.luna5ama.trollhack.utils.timing.TickTimer
import dev.luna5ama.trollhack.utils.rotation.Priority
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundAttackPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.*

object CrystalUtils {
    private val breakTimer = TickTimer()

    private val cacheBlockPos = ThreadLocal.withInitial {
        BlockPos.MutableBlockPos()
    }

    fun getDirection2D(d: Double, d2: Double): Double {
        var d3: Double
        if (d2 == 0.0) {
            d3 = if (d > 0.0) 90.0 else -90.0
        } else {
            d3 = atan(d / d2) * 57.2957796
            if (d2 < 0.0) {
                d3 = if (d > 0.0) 180.0.let { d3 += it; d3 } else if (d < 0.0) 180.0.let { d3 -= it; d3 } else 180.0
            }
        }
        return d3
    }

    context(ctx: NonNullContext)
    fun canPlaceCrystal(pos: BlockPos, entity: LivingEntity? = null): Boolean = ctx.run {
        return canPlaceCrystalOn(pos)
                && (entity == null || !getCrystalPlacingBB(pos).intersects(entity.boundingBox))
                && hasValidSpaceForCrystal(pos, true)
    }

    context(ctx: NonNullContext)
    fun attackCrystal(box: AABB, rotate: Boolean, eatingPause: Boolean): Unit = ctx.run {
        for (entity in world.getEntitiesOfClass(EndCrystal::class.java, box)) {
            attackCrystal(entity, rotate, eatingPause)
            break
        }
    }

    context(ctx: NonNullContext)
    fun attackCrystal(crystal: Entity, rotate: Boolean, usingPause: Boolean): Unit = ctx.run {
        if (!breakTimer.tickAndReset(1000)) return
        if (usingPause && player.isUsingItem) return
        if (rotate) {
            RotationManager.setRotations(
                RotationUtils.getRotationTo(Vec3(crystal.x, crystal.y + 0.25, crystal.z)),
                priority = Priority.High
            )
        }
        netHandler.send(ServerboundAttackPacket(crystal.id))
        player.swing(InteractionHand.MAIN_HAND)
    }

    fun getVectorForRotation(d: Double, d2: Double): Vec3 {
        val f = cos(-(d2.toRadian() + PI).toFloat())
        val f2 = sin(-(d2.toRadian() + PI).toFloat())
        val f3 = -cos(-d.toRadian().toFloat())
        val f4 = sin(-d.toRadian().toFloat())
        return Vec3((f2 * f3).toDouble(), f4.toDouble(), (f * f3).toDouble())
    }

    fun getRange(a: Vec3, x: Double, y: Double, z: Double): Double {
        val xl = a.x - x
        val yl = a.y - y
        val zl = a.z - z
        return sqrt(xl * xl + yl * yl + zl * zl)
    }

    context(ctx: NonNullContext)
    fun hasValidSpaceForCrystal(pos: BlockPos): Boolean = ctx.run {
        return hasValidSpaceForCrystal(pos, false)
    }

    context(ctx: NonNullContext)
    fun hasValidSpaceForCrystal(pos: BlockPos, newPlace: Boolean): Boolean = ctx.run {
        val mutableBlockPos = BlockPos.MutableBlockPos()
        return isValidMaterial(mutableBlockPos.setAndAdd(pos, 0, 1, 0).state)
                && (isValidMaterial(mutableBlockPos.offset(0, 1, 0).state) || newPlace)
    }

    context(ctx: NonNullContext)
    fun canPlaceCrystalOn(pos: BlockPos): Boolean = ctx.run {
        val block = world.getBlockState(pos).block
        return block == Blocks.BEDROCK || block == Blocks.OBSIDIAN
    }

    fun isValidMaterial(blockState: BlockState): Boolean {
        return !blockState.liquid() && blockState.canBeReplaced()
    }

    fun getCrystalPlacingBB(pos: BlockPos): AABB {
        return getCrystalPlacingBB(pos.x, pos.y, pos.z)
    }

    fun getCrystalPlacingBB(x: Int, y: Int, z: Int): AABB {
        return AABB(
            x + 0.001, y + 1.0, z + 0.001,
            x + 0.999, y + 3.0, z + 0.999
        )
    }

    fun getCrystalPlacingBB(pos: Vec3): AABB {
        return getCrystalPlacingBB(pos.x, pos.y, pos.z)
    }

    fun getCrystalPlacingBB(x: Double, y: Double, z: Double): AABB {
        return AABB(
            x - 0.499, y, z - 0.499,
            x + 0.499, y + 2.0, z + 0.499
        )
    }

    fun getCrystalBB(pos: BlockPos): AABB {
        return getCrystalBB(pos.x, pos.y, pos.z)
    }

    fun getCrystalBB(x: Int, y: Int, z: Int): AABB {
        return AABB(
            x - 0.5, y + 1.0, z - 0.5,
            x + 1.5, y + 3.0, z + 1.5
        )
    }

    fun getCrystalBB(pos: Vec3): AABB {
        return getCrystalBB(pos.x, pos.y, pos.z)
    }

    fun getCrystalBB(x: Double, y: Double, z: Double): AABB {
        return AABB(
            x - 1.0, y, z - 1.0,
            x + 1.0, y + 2.0, z + 1.0
        )
    }

    fun placeBoxIntersectsCrystalBox(placePos: BlockPos, crystalPos: BlockPos): Boolean {
        return crystalPos.y - placePos.y in 0..2
                && abs(crystalPos.x - placePos.x) < 2
                && abs(crystalPos.z - placePos.z) < 2
    }

    fun placeBoxIntersectsCrystalBox(placePos: Vec3, crystalPos: BlockPos): Boolean {
        return crystalPos.y - placePos.y in 0.0..2.0
                && abs(crystalPos.x - placePos.x) < 2.0
                && abs(crystalPos.z - placePos.z) < 2.0
    }

    fun placeBoxIntersectsCrystalBox(placeX: Double, placeY: Double, placeZ: Double, crystalPos: BlockPos): Boolean {
        return crystalPos.y - placeY in 0.0..2.0
                && abs(crystalPos.x - placeX) < 2.0
                && abs(crystalPos.z - placeZ) < 2.0
    }

    private fun Double.withIn(a: Double, b: Double): Boolean {
        return this > a && this < b
    }

    private fun Int.withIn(a: Int, b: Int): Boolean {
        return this in a..b
    }

    fun crystalPlaceBoxIntersectsCrystalBox(placePos: BlockPos, crystalPos: Vec3): Boolean {
        return crystalPlaceBoxIntersectsCrystalBox(placePos, crystalPos.x, crystalPos.y, crystalPos.z)
    }

    fun crystalPlaceBoxIntersectsCrystalBox(placePos: BlockPos, crystal: EndCrystal): Boolean {
        return crystalPlaceBoxIntersectsCrystalBox(placePos, crystal.x, crystal.y, crystal.z)
    }

    fun crystalPlaceBoxIntersectsCrystalBox(
        placePos: BlockPos,
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double
    ): Boolean {
        return (crystalY.floorToInt() - placePos.y).withIn(0, 2)
                && (crystalX.floorToInt() - placePos.x).withIn(-1, 1)
                && (crystalZ.floorToInt() - placePos.z).withIn(-1, 1)
    }

    fun placeBoxIntersectsCrystalBox(
        placeX: Double, placeY: Double, placeZ: Double,
        crystalX: Double, crystalY: Double, crystalZ: Double
    ): Boolean {
        return crystalY - placeY in 0.0..2.0
                && abs(crystalX - placeX) < 2.0
                && abs(crystalZ - placeZ) < 2.0
    }

    @Suppress("DEPRECATION")
    fun isResistant(blockState: BlockState) =
        !blockState.liquid() && blockState.block.explosionResistance >= 19.7
}
