package dev.luna5ama.trollhack.utils.combat

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.core.BlockPos
import java.util.ArrayList

object BlockInteractionHelper {
    val blackList: List<Block> = listOf(
        Blocks.ENDER_CHEST,
        Blocks.CHEST as Block,
        Blocks.TRAPPED_CHEST,
        Blocks.CRAFTING_TABLE,
        Blocks.ANVIL,
        Blocks.BREWING_STAND,
        Blocks.HOPPER as Block,
        Blocks.DROPPER,
        Blocks.DISPENSER,
        Blocks.OAK_TRAPDOOR,
        Blocks.SPRUCE_TRAPDOOR,
        Blocks.BIRCH_TRAPDOOR,
        Blocks.JUNGLE_TRAPDOOR,
        Blocks.ACACIA_TRAPDOOR,
        Blocks.CHERRY_TRAPDOOR,
        Blocks.DARK_OAK_TRAPDOOR,
        Blocks.MANGROVE_TRAPDOOR,
        Blocks.BAMBOO_TRAPDOOR,
        Blocks.ENCHANTING_TABLE
    )

    fun getSphere(loc: BlockPos, r: Float, h: Int, hollow: Boolean, sphere: Boolean, plusY: Int): List<BlockPos> {
        val circleBlocks: MutableList<BlockPos> = ArrayList()
        val cx = loc.x
        val cy = loc.y
        val cz = loc.z
        val radiusSquared = r * r
        (cx - r.toInt()..cx + r.toInt()).forEach { x ->
            (cz - r.toInt()..cz + r.toInt()).forEach { z ->
                val start = if (sphere) cy - r.toInt() else cy
                val end = if (sphere) cy + r else cy + h
                (start until end.toInt()).forEach { y ->
                    val dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + if (sphere) (cy - y) * (cy - y) else 0
                    val isInside = dist < radiusSquared && (!hollow || dist >= (r - 1.0f) * (r - 1.0f))
                    if (isInside) {
                        val blockPos = BlockPos(x, y + plusY, z)
                        circleBlocks.add(blockPos)
                    }
                }
            }
        }
        return circleBlocks
    }
}