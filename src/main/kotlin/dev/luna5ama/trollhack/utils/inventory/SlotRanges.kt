package dev.luna5ama.trollhack.utils.inventory

import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.utils.NonNullContext
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.animal.equine.Donkey
import net.minecraft.world.entity.animal.equine.Horse
import net.minecraft.world.entity.animal.equine.Llama
import net.minecraft.world.entity.animal.equine.SkeletonHorse
import net.minecraft.world.entity.animal.equine.ZombieHorse
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.AbstractMountInventoryMenu
import net.minecraft.world.inventory.AnvilMenu
import net.minecraft.world.inventory.BeaconMenu
import net.minecraft.world.inventory.BlastFurnaceMenu
import net.minecraft.world.inventory.BrewingStandMenu
import net.minecraft.world.inventory.CartographyTableMenu
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.CraftingMenu
import net.minecraft.world.inventory.DispenserMenu
import net.minecraft.world.inventory.EnchantmentMenu
import net.minecraft.world.inventory.FurnaceMenu
import net.minecraft.world.inventory.GrindstoneMenu
import net.minecraft.world.inventory.HopperMenu
import net.minecraft.world.inventory.HorseInventoryMenu
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.inventory.LecternMenu
import net.minecraft.world.inventory.LoomMenu
import net.minecraft.world.inventory.MerchantMenu
import net.minecraft.world.inventory.ShulkerBoxMenu
import net.minecraft.world.inventory.SmokerMenu
import net.minecraft.world.inventory.StonecutterMenu
import net.minecraft.world.item.CreativeModeTabs

object SlotRanges {
    val HOTBAR = 0..8
    val MAIN = 9..35
    val ARMOR = 36..39
    const val OFFHAND = 45

    context(ctx: NonNullContext)
    fun indexToId(i: Int): Int = ctx.run {
        val handler: AbstractContainerMenu = player.containerMenu
        return when (handler) {
            is InventoryMenu -> survivalInventory(i)
            is CreativeModeInventoryScreen.ItemPickerMenu -> creativeInventory(i)
            is ChestMenu -> genericContainer(i, handler.rowCount)
            is CraftingMenu -> craftingTable(i)
            is FurnaceMenu -> furnace(i)
            is BlastFurnaceMenu -> furnace(i)
            is SmokerMenu -> furnace(i)
            is DispenserMenu -> generic3x3(i)
            is EnchantmentMenu -> enchantmentTable(i)
            is BrewingStandMenu -> brewingStand(i)
            is MerchantMenu -> villager(i)
            is BeaconMenu -> beacon(i)
            is AnvilMenu -> anvil(i)
            is HopperMenu -> hopper(i)
            is ShulkerBoxMenu -> genericContainer(i, 3)
            is HorseInventoryMenu -> horse(handler, i)
            is CartographyTableMenu -> cartographyTable(i)
            is GrindstoneMenu -> grindstone(i)
            is LecternMenu -> lectern()
            is LoomMenu -> loom(i)
            is StonecutterMenu -> stonecutter(i)
            else -> -1
        }
    }

    private fun survivalInventory(i: Int): Int {
        if (isHotbar(i)) return 36 + i
        return if (isArmor(i)) 5 + (i - 36) else i
    }

    private fun creativeInventory(i: Int): Int {
        return if (mc.screen !is CreativeModeInventoryScreen
            || CreativeModeInventoryScreen.selectedTab != BuiltInRegistries.CREATIVE_MODE_TAB[CreativeModeTabs.INVENTORY]) -1
        else survivalInventory(i)
    }

    private fun genericContainer(i: Int, rows: Int): Int {
        if (isHotbar(i)) return (rows + 3) * 9 + i
        return if (isMain(i)) rows * 9 + (i - 9) else -1
    }

    private fun craftingTable(i: Int): Int {
        if (isHotbar(i)) return 37 + i
        return if (isMain(i)) i + 1 else -1
    }

    private fun furnace(i: Int): Int {
        if (isHotbar(i)) return 30 + i
        return if (isMain(i)) 3 + (i - 9) else -1
    }

    private fun generic3x3(i: Int): Int {
        if (isHotbar(i)) return 36 + i
        return if (isMain(i)) i else -1
    }

    private fun enchantmentTable(i: Int): Int {
        if (isHotbar(i)) return 29 + i
        return if (isMain(i)) 2 + (i - 9) else -1
    }

    private fun brewingStand(i: Int): Int {
        if (isHotbar(i)) return 32 + i
        return if (isMain(i)) 5 + (i - 9) else -1
    }

    private fun villager(i: Int): Int {
        if (isHotbar(i)) return 30 + i
        return if (isMain(i)) 3 + (i - 9) else -1
    }

    private fun beacon(i: Int): Int {
        if (isHotbar(i)) return 28 + i
        return if (isMain(i)) 1 + (i - 9) else -1
    }

    private fun anvil(i: Int): Int {
        if (isHotbar(i)) return 30 + i
        return if (isMain(i)) 3 + (i - 9) else -1
    }

    private fun hopper(i: Int): Int {
        if (isHotbar(i)) return 32 + i
        return if (isMain(i)) 5 + (i - 9) else -1
    }

    private fun horse(handler: AbstractContainerMenu, i: Int): Int {
        val entity = (handler as AbstractMountInventoryMenu).mount
        if (entity is Llama) {
            val strength: Int = entity.strength
            if (isHotbar(i)) return 2 + 3 * strength + 28 + i
            if (isMain(i)) return 2 + 3 * strength + 1 + (i - 9)
        } else if (entity is Horse || entity is SkeletonHorse || entity is ZombieHorse) {
            if (isHotbar(i)) return 29 + i
            if (isMain(i)) return 2 + (i - 9)
        } else if (entity is Donkey) {
            val chest: Boolean = entity.hasChest()
            if (isHotbar(i)) return (if (chest) 44 else 29) + i
            if (isMain(i)) return (if (chest) 17 else 2) + (i - 9)
        }
        return -1
    }

    private fun cartographyTable(i: Int): Int {
        if (isHotbar(i)) return 30 + i
        return if (isMain(i)) 3 + (i - 9) else -1
    }

    private fun grindstone(i: Int): Int {
        if (isHotbar(i)) return 30 + i
        return if (isMain(i)) 3 + (i - 9) else -1
    }

    private fun lectern(): Int {
        return -1
    }

    private fun loom(i: Int): Int {
        if (isHotbar(i)) return 31 + i
        return if (isMain(i)) 4 + (i - 9) else -1
    }

    private fun stonecutter(i: Int): Int {
        if (isHotbar(i)) return 29 + i
        return if (isMain(i)) 2 + (i - 9) else -1
    }

    fun isHotbar(i: Int): Boolean {
        return i in HOTBAR
    }

    fun isMain(i: Int): Boolean {
        return i in MAIN
    }

    fun isArmor(i: Int): Boolean {
        return i in ARMOR
    }
}
