package dev.luna5ama.trollhack.util.world

import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.inventory.blockBlacklist
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

val IBlockState.isBlacklisted: Boolean
    get() = blockBlacklist.contains(this.block)

val IBlockState.isLiquid: Boolean
    get() = this.material.isLiquid

val IBlockState.isWater: Boolean
    get() = this.block == Blocks.WATER

val IBlockState.isReplaceable: Boolean
    get() = this.material.isReplaceable

val IBlockState.isFullBox: Boolean
    get() = Wrapper.world?.let {
        this.getCollisionBoundingBox(it, BlockPos.ORIGIN)
    } == Block.FULL_BLOCK_AABB

fun World.getBlockState(x: Int, y: Int, z: Int): IBlockState {
    return if (y !in 0..255) {
        Blocks.AIR.defaultState
    } else {
        val chunk = getChunk(x shr 4, z shr 4)
        return if (chunk.isEmpty) Blocks.AIR.defaultState else chunk.getBlockState(x, y, z)
    }
}

val IBlockState.isAir get() = material == Material.AIR

fun World.isAir(x: Int, y: Int, z: Int): Boolean {
    return getBlockState(x, y, z).isAir
}

fun World.isAir(pos: BlockPos): Boolean {
    return getBlockState(pos).isAir
}

fun World.getBlock(pos: BlockPos): Block =
    this.getBlockState(pos).block

fun World.getMaterial(pos: BlockPos): Material =
    this.getBlockState(pos).material

fun World.getSelectedBox(pos: BlockPos): AxisAlignedBB =
    this.getBlockState(pos).getSelectedBoundingBox(this, pos)

fun World.getCollisionBox(pos: BlockPos): AxisAlignedBB? =
    this.getBlockState(pos).getCollisionBoundingBox(this, pos)

fun World.hasCollisionBox(pos: BlockPos): Boolean = this.getCollisionBox(pos) != null