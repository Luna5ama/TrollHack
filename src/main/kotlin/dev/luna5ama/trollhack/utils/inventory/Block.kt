package dev.luna5ama.trollhack.utils.inventory

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks.*

val shulkerList: Set<Block> = hashSetOf(
    WHITE_SHULKER_BOX,
    ORANGE_SHULKER_BOX,
    MAGENTA_SHULKER_BOX,
    LIGHT_BLUE_SHULKER_BOX,
    YELLOW_SHULKER_BOX,
    LIME_SHULKER_BOX,
    PINK_SHULKER_BOX,
    GRAY_SHULKER_BOX,
    LIGHT_GRAY_SHULKER_BOX,
    CYAN_SHULKER_BOX,
    PURPLE_SHULKER_BOX,
    BLUE_SHULKER_BOX,
    BROWN_SHULKER_BOX,
    GREEN_SHULKER_BOX,
    RED_SHULKER_BOX,
    BLACK_SHULKER_BOX
)

val blockBlacklist: Set<Block> = hashSetOf(
    ENDER_CHEST,
    CHEST,
    TRAPPED_CHEST,
    CRAFTING_TABLE,
    ANVIL,
    BREWING_STAND,
    HOPPER,
    DROPPER,
    DISPENSER,
    OAK_TRAPDOOR,
    SPRUCE_TRAPDOOR,
    BIRCH_TRAPDOOR,
    JUNGLE_TRAPDOOR,
    ACACIA_TRAPDOOR,
    CHERRY_TRAPDOOR,
    DARK_OAK_TRAPDOOR,
    MANGROVE_TRAPDOOR,
    BAMBOO_TRAPDOOR,
    ENCHANTING_TABLE
).apply {
    addAll(shulkerList)
}