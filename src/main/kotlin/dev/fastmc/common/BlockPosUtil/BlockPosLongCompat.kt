@file:Suppress("unused")

package dev.fastmc.common.BlockPosUtil

import net.minecraft.core.BlockPos

fun BlockPos.toLong(): Long = this.asLong()

fun toLong(x: Int, y: Int, z: Int): Long = BlockPos.asLong(x, y, z)
