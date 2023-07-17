package dev.luna5ama.trollhack.util.combat

import dev.fastmc.common.floorToInt
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.module.modules.combat.CombatSetting
import dev.luna5ama.trollhack.util.math.VectorUtils.setAndAdd
import dev.luna5ama.trollhack.util.world.isAir
import dev.luna5ama.trollhack.util.world.isLiquid
import dev.luna5ama.trollhack.util.world.isReplaceable
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.init.Blocks
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.function.Predicate
import kotlin.math.abs

object CrystalUtils {
    val EntityEnderCrystal.blockPos: BlockPos
        get() = BlockPos(this.posX.floorToInt(), this.posY.floorToInt() - 1, this.posZ.floorToInt())

    private val cacheBlockPos = ThreadLocal.withInitial {
        BlockPos.MutableBlockPos()
    }

    /** Checks colliding with blocks and given entity */
    fun SafeClientEvent.canPlaceCrystal(
        pos: BlockPos, entity: EntityLivingBase? = null,
        mutableBlockPos: BlockPos.MutableBlockPos = cacheBlockPos.get()
    ): Boolean {
        return canPlaceCrystalOn(pos)
            && (entity == null || !getCrystalPlacingBB(pos).intersects(entity.entityBoundingBox))
            && hasValidSpaceForCrystal(pos, mutableBlockPos)
    }

    /** Checks if the block is valid for placing crystal */
    fun SafeClientEvent.canPlaceCrystalOn(pos: BlockPos): Boolean {
        val block = world.getBlockState(pos).block
        return block == Blocks.BEDROCK || block == Blocks.OBSIDIAN
    }

    fun SafeClientEvent.hasValidSpaceForCrystal(
        pos: BlockPos,
        mutableBlockPos: BlockPos.MutableBlockPos = cacheBlockPos.get()
    ): Boolean {
        return if (CombatSetting.newCrystalPlacement) {
            world.isAir(mutableBlockPos.setAndAdd(pos, 0, 1, 0))
        } else {
            isValidMaterial(world.getBlockState(mutableBlockPos.setAndAdd(pos, 0, 1, 0)))
                && isValidMaterial(world.getBlockState(mutableBlockPos.add(0, 1, 0)))
        }
    }

    fun isValidMaterial(blockState: IBlockState): Boolean {
        return !blockState.isLiquid && blockState.isReplaceable
    }

    fun getCrystalPlacingBB(pos: BlockPos): AxisAlignedBB {
        return getCrystalPlacingBB(pos.x, pos.y, pos.z)
    }

    fun getCrystalPlacingBB(x: Int, y: Int, z: Int): AxisAlignedBB {
        val margin = CombatSetting.collisionMargin.toDouble()
        return AxisAlignedBB(
            x + margin, y + 1.0, z + margin,
            x + 1.0 - margin, y + 3.0, z + 1.0 - margin
        )
    }

    fun getCrystalPlacingBB(pos: Vec3d): AxisAlignedBB {
        return getCrystalPlacingBB(pos.x, pos.y, pos.z)
    }

    fun getCrystalPlacingBB(x: Double, y: Double, z: Double): AxisAlignedBB {
        return AxisAlignedBB(
            x - 0.499, y, z - 0.499,
            x + 0.499, y + 2.0, z + 0.499
        )
    }

    fun getCrystalBB(pos: BlockPos): AxisAlignedBB {
        return getCrystalBB(pos.x, pos.y, pos.z)
    }

    fun getCrystalBB(x: Int, y: Int, z: Int): AxisAlignedBB {
        return AxisAlignedBB(
            x - 0.5, y + 1.0, z - 0.5,
            x + 1.5, y + 3.0, z + 1.5
        )
    }

    fun getCrystalBB(pos: Vec3d): AxisAlignedBB {
        return getCrystalBB(pos.x, pos.y, pos.z)
    }

    fun getCrystalBB(x: Double, y: Double, z: Double): AxisAlignedBB {
        return AxisAlignedBB(
            x - 1.0, y, z - 1.0,
            x + 1.0, y + 2.0, z + 1.0
        )
    }

    fun crystalPlaceBoxIntersectsCrystalBox(placePos: BlockPos, crystalPos: Vec3d): Boolean {
        return crystalPlaceBoxIntersectsCrystalBox(placePos, crystalPos.x, crystalPos.y, crystalPos.z)
    }

    fun crystalPlaceBoxIntersectsCrystalBox(placePos: BlockPos, crystal: EntityEnderCrystal): Boolean {
        return crystalPlaceBoxIntersectsCrystalBox(placePos, crystal.posX, crystal.posY, crystal.posZ)
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


    fun blockPlaceBoxIntersectsCrystalBox(placePos: BlockPos, crystal: EntityEnderCrystal): Boolean {
        return blockPlaceBoxIntersectsCrystalBox(placePos, crystal.posX, crystal.posY, crystal.posZ)
    }

    fun blockPlaceBoxIntersectsCrystalBox(
        placePos: BlockPos,
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double
    ): Boolean {
        return (crystalY.floorToInt() - placePos.y).withIn(0, 1)
            && (crystalX.floorToInt() - placePos.x).withIn(-1, 1)
            && (crystalZ.floorToInt() - placePos.z).withIn(-1, 1)
    }


    fun crystalIntersects(crystal1: BlockPos, crystal2: BlockPos): Boolean {
        return crystalIntersects(crystal1.x, crystal1.y, crystal1.z, crystal2.x, crystal2.y, crystal2.z)
    }

    fun crystalIntersects(crystal1: BlockPos, crystal2: Vec3d): Boolean {
        return crystalIntersects(crystal1.x, crystal1.y, crystal1.z, crystal2.x, crystal2.y, crystal2.z)
    }

    fun crystalIntersects(crystal1: Vec3d, crystal2: BlockPos): Boolean {
        return crystalIntersects(crystal2.x, crystal2.y, crystal2.z, crystal1.x, crystal1.y, crystal1.z)
    }

    fun crystalIntersects(
        crystal1X: Int,
        crystal1Y: Int,
        crystal1Z: Int,
        crystal2X: Int,
        crystal2Y: Int,
        crystal2Z: Int
    ): Boolean {
        return abs(crystal2Y - crystal1Y) < 2
            && abs(crystal2X - crystal1X) < 2
            && abs(crystal2Z - crystal1Z) < 2
    }

    fun crystalIntersects(crystal1: EntityEnderCrystal, crystal2: BlockPos): Boolean {
        return crystalIntersects(crystal2.x, crystal2.y, crystal2.z, crystal1.posX, crystal1.posY, crystal1.posZ)
    }

    fun crystalIntersects(
        crystal1X: Int,
        crystal1Y: Int,
        crystal1Z: Int,
        crystal2X: Double,
        crystal2Y: Double,
        crystal2Z: Double
    ): Boolean {
        return abs(crystal2Y - (crystal1Y + 1)) < 2.0
            && abs(crystal2X - (crystal1X + 0.5)) < 2.0
            && abs(crystal2Z - (crystal1Z + 0.5)) < 2.0
    }

    fun crystalIntersects(crystal1: Vec3d, crystal2: Vec3d): Boolean {
        return crystalIntersects(crystal1.x, crystal1.y, crystal1.z, crystal2.x, crystal2.y, crystal2.z)
    }

    fun crystalIntersects(
        crystal1X: Double,
        crystal1Y: Double,
        crystal1Z: Double,
        crystal2X: Double,
        crystal2Y: Double,
        crystal2Z: Double
    ): Boolean {
        return abs(crystal2Y - crystal1Y) < 2.0
            && abs(crystal2X - crystal1X) < 2.0
            && abs(crystal2Z - crystal1Z) < 2.0
    }

    private fun Double.withIn(a: Double, b: Double): Boolean {
        return this > a && this < b
    }

    private fun Int.withIn(a: Int, b: Int): Boolean {
        return this in a..b
    }

    /** Checks colliding with All Entities */
    fun placeCollideCheck(pos: BlockPos): Boolean {
        val placingBB = getCrystalPlacingBB(pos)
        return EntityManager.entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.entityBoundingBox.intersects(placingBB) }
            .none()
    }

    fun placeCollideCheck(pos: BlockPos, predicate: Predicate<Entity>): Boolean {
        val placingBB = getCrystalPlacingBB(pos)
        return EntityManager.entity.asSequence()
            .filter { it.isEntityAlive }
            .filter { it.entityBoundingBox.intersects(placingBB) }
            .filterNot { predicate.test(it) }
            .none()
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")
    fun isResistant(blockState: IBlockState): Boolean {
        return blockState.material == Blocks.AIR || !blockState.isLiquid && blockState.block.getExplosionResistance(null) >= 19.7
    }
}