package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.setting.settings.impl.collection.CollectionSetting
import cum.xiaro.trollhack.util.BOOLEAN_SUPPLIER_FALSE
import cum.xiaro.trollhack.util.threads.onMainThread
import net.minecraft.block.state.IBlockState

internal object Xray : Module(
    name = "Xray",
    description = "Lets you see through blocks",
    category = Category.RENDER
) {
    private val defaultVisibleList = linkedSetOf("minecraft:diamond_ore", "minecraft:iron_ore", "minecraft:gold_ore", "minecraft:portal", "minecraft:cobblestone")

    val blockList = setting(CollectionSetting("Visible List", defaultVisibleList, BOOLEAN_SUPPLIER_FALSE))

    @JvmStatic
    fun shouldReplace(state: IBlockState): Boolean {
        return isEnabled && !blockList.contains(state.block.registryName.toString())
    }

    init {
        onToggle {
            onMainThread {
                mc.renderGlobal?.loadRenderers()
            }
        }

        blockList.editListeners.add {
            onMainThread {
                mc.renderGlobal?.loadRenderers()
            }
        }
    }
}
