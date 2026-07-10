package dev.luna5ama.trollhack.utils.extension

import dev.luna5ama.trollhack.utils.NonNullContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.core.BlockPos

context(ctx: NonNullContext)
val BlockPos.state: BlockState
    get() = ctx.world.getBlockState(this)

context(ctx: NonNullContext)
val BlockPos.block: Block
    get() = state.block

context(ctx: NonNullContext)
val BlockPos.isAir: Boolean
    get() = this.block == Blocks.AIR
