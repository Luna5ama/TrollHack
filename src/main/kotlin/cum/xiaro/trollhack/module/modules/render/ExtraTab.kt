package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import net.minecraft.client.network.NetworkPlayerInfo

internal object ExtraTab : Module(
    name = "ExtraTab",
    description = "Expands the player tab menu",
    category = Category.RENDER
) {
    private val tabSize by setting("Max Players", 265, 80..400, 5)

    override fun getHudInfo(): String {
        return tabSize.toString()
    }

    @JvmStatic
    fun subList(list: List<NetworkPlayerInfo>, newList: List<NetworkPlayerInfo>): List<NetworkPlayerInfo> {
        return if (isDisabled) newList else list.subList(0, tabSize.coerceAtMost(list.size))
    }
}