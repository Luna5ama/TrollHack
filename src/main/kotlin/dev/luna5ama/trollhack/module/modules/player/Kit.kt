package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.setting.settings.impl.collection.MapSetting
import dev.luna5ama.trollhack.util.BOOLEAN_SUPPLIER_FALSE
import dev.luna5ama.trollhack.util.inventory.regName
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import java.util.*

internal object Kit : Module(
    name = "Kit",
    category = Category.PLAYER,
    description = "Setting up kit management",
    alwaysEnabled = true,
    visible = false
) {
    val kitMap = setting(MapSetting("Kit", TreeMap<String, List<String>>(), BOOLEAN_SUPPLIER_FALSE))
    var kitName by setting("Kit Name", "None")

    fun getKitItemArray(): Array<ItemEntry>? {
        val stringArray = kitMap.value[kitName.lowercase()] ?: return null

        return Array(36) { index ->
            stringArray.getOrNull(index)?.let { ItemEntry.fromString(it) } ?: ItemEntry.EMPTY
        }
    }

    data class ItemEntry(val item: Item, val name: String?) {
        override fun toString(): String {
            return "${item.regName} $name"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            return when (other) {
                is ItemEntry -> item == other.item && (name == null || name == other.name)
                is ItemStack -> item == other.item && (name == null || name == other.displayName)
                is Item -> item == other
                else -> false
            }
        }

        override fun hashCode(): Int {
            var result = item.hashCode()
            result = 31 * result + (name?.hashCode() ?: 0)
            return result
        }

        companion object {
            @JvmField
            val EMPTY = ItemEntry(Items.AIR, null)

            fun fromString(string: String): ItemEntry {
                val index = string.indexOf(' ')
                val itemRegName = string.substring(0, if (index == -1) string.length else index)
                val item = Item.getByNameOrId(itemRegName) ?: Items.AIR
                val name = if (index == -1) null else string.substring(index + 1)
                return ItemEntry(item, name)
            }

            fun fromStack(stack: ItemStack): ItemEntry {
                return ItemEntry(stack.item, stack.displayName)
            }
        }
    }
}