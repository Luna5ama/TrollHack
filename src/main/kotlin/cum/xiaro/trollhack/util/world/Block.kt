@file:Suppress("NOTHING_TO_INLINE")

package cum.xiaro.trollhack.util.world

import cum.xiaro.trollhack.util.Wrapper
import cum.xiaro.trollhack.util.items.blockBlacklist
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.init.Blocks
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

inline val IBlockState.isBlacklisted: Boolean
    get() = blockBlacklist.contains(this.block)

inline val IBlockState.isLiquid: Boolean
    get() = this.material.isLiquid

inline val IBlockState.isWater: Boolean
    get() = this.block == Blocks.WATER

inline val IBlockState.isReplaceable: Boolean
    get() = this.material.isReplaceable

inline val IBlockState.isFullBox: Boolean
    get() = Wrapper.world?.let {
        this.getCollisionBoundingBox(it, BlockPos.ORIGIN)
    } == Block.FULL_BLOCK_AABB

inline fun World.getBlockState(x: Int, y: Int, z: Int): IBlockState {
    return if (y !in 0..255) {
        Blocks.AIR.defaultState
    } else {
        val chunk = getChunk(x shr 4, z shr 4)
        return if (chunk.isEmpty) Blocks.AIR.defaultState else chunk.getBlockState(x, y, z)
    }
}

inline fun World.isAir(x: Int, y: Int, z: Int): Boolean {
    return getBlockState(x, y, z).block == Blocks.AIR
}

inline fun World.isAir(pos: BlockPos): Boolean {
    return getBlockState(pos).block == Blocks.AIR
}

inline fun World.getBlock(pos: BlockPos): Block =
    this.getBlockState(pos).block

inline fun World.getMaterial(pos: BlockPos): Material =
    this.getBlockState(pos).material

inline fun WorldClient.getSelectedBox(pos: BlockPos): AxisAlignedBB =
    this.getBlockState(pos).getSelectedBoundingBox(this, pos)

inline fun WorldClient.getCollisionBox(pos: BlockPos): AxisAlignedBB? =
    this.getBlockState(pos).getCollisionBoundingBox(this, pos)