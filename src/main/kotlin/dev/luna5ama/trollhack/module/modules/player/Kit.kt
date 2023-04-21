package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.setting.settings.impl.collection.MapSetting
import dev.luna5ama.trollhack.util.BOOLEAN_SUPPLIER_FALSE
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