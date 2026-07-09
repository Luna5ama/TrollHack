package dev.luna5ama.trollhack.utils.world

import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.extension.entry
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.core.BlockPos
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Mob
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import kotlin.math.max

object MineUtils {
    context (NonNullContext)
    fun findBestTool(pos: BlockPos): Int {
        return findBestTool(pos, world.getBlockState(pos))
    }

    context (NonNullContext)
    fun findBestTool2(pos: BlockPos): Int {
        return findBestTool2(pos, world.getBlockState(pos))
    }

    context (NonNullContext)
    fun findBestTool(pos: BlockPos, state: BlockState): Int {
        var result: Int = player.inventory.selectedSlot
        if (state.getDestroySpeed(world, pos) > 0) {
            var speed = getSpeed(state, player.mainHandItem)
            for (i in 0..8) {
                val stack = player.inventory.getItem(i)
                val stackSpeed = getSpeed(state, stack)
                if (stackSpeed > speed) {
                    speed = stackSpeed
                    result = i
                }
            }
        }
        return result
    }

    context (NonNullContext)
    fun findBestTool2(pos: BlockPos, state: BlockState): Int {
        var result: Int = player.inventory.selectedSlot
        if (state.getDestroySpeed(world, pos) > 0) {
            var speed = getSpeed(state, player.mainHandItem)
            for (i in 0..34) {
                val stack = player.inventory.getItem(i)
                val stackSpeed = getSpeed(state, stack)
                if (stackSpeed > speed) {
                    speed = stackSpeed
                    result = i
                }
            }
        }
        return result
    }

    fun getSpeed(state: BlockState, stack: ItemStack): Double {
        val str = stack.getDestroySpeed(state).toDouble()
        val effect = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.EFFICIENCY.entry, stack)
        return max(str + if (str > 1.0) effect * effect + 1.0 else 0.0, 0.0)
    }

    context (NonNullContext)
    fun getBlockStrength(position: BlockPos, itemStack: ItemStack, onGround: Boolean): Float {
        val state: BlockState = world.getBlockState(position)
        val hardness = state.getDestroySpeed(world, position)
        if (hardness < 0) {
            return 0.0F
        }
        return if (!canBreak(position)) {
            getDigSpeed(itemStack,state,onGround) / hardness / 100f
        } else {
            getDigSpeed(itemStack,state,onGround) / hardness / 30f
        }
    }

    context (NonNullContext)
    fun getDigSpeed(stack: ItemStack, state: BlockState, onGround: Boolean): Float {
        var digSpeed  = getSpeed(state, stack).toFloat()

        if (digSpeed > 1.0f) {
            val i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.EFFICIENCY.entry, stack)
            if (i > 0 && !stack.isEmpty) {
                digSpeed += (i * i + 1).toFloat()
            }
        }

        if (player.hasEffect(MobEffects.HASTE)) {
            digSpeed *= (1.0f + (player.getEffect(MobEffects.HASTE)!!.amplifier + 1) * 0.2f)
        }
        if (player.hasEffect(MobEffects.MINING_FATIGUE)) {
            val miningFatigue: Float = when (player.getEffect(MobEffects.MINING_FATIGUE)!!.amplifier) {
                0 -> 0.3f
                1 -> 0.09f
                2 -> 0.0027f
                3 -> 8.1E-4f
                else -> 8.1E-4f
            }
            digSpeed *= miningFatigue
        }
//        if (player.isSubmergedInWater
//
//            && !EnchantmentHelper.hasAquaAffinity(mc.player)) {
//            digSpeed /= 5.0f
//        }

        if (onGround && !player.onGround()) {
            digSpeed /= 5.0f
        }
        return if (digSpeed < 0) 0f else digSpeed
    }

    context (NonNullContext)
    fun canBreak(pos: BlockPos): Boolean {
        return canBreak(world.getBlockState(pos), pos)
    }

    context (NonNullContext)
    fun canBreak(state: BlockState, pos: BlockPos): Boolean {
        return (state.getDestroySpeed(world, pos) != -1f
                || state.fluidState.isSource)

    }
}