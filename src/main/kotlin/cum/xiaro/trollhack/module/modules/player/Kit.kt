package cum.xiaro.trollhack.module.modules.player

import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.setting.settings.impl.collection.MapSetting
import cum.xiaro.trollhack.util.BOOLEAN_SUPPLIER_FALSE
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