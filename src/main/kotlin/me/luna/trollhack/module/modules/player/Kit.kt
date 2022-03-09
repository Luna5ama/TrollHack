package me.luna.trollhack.module.modules.player

import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.setting.settings.impl.collection.MapSetting
import me.luna.trollhack.util.BOOLEAN_SUPPLIER_FALSE
import net.minecraft.init.Items
import net.minecraft.item.Item
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

    fun getKitItemArray(): Array<Item>? {
        val stringArray = kitMap.value[kitName.lowercase()] ?: return null

        return Array(36) { index ->
            stringArray.getOrNull(index)?.let {
                Item.getByNameOrId(it)
            } ?: Items.AIR
        }
    }
}