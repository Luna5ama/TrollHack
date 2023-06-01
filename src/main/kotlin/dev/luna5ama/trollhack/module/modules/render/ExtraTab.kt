package dev.luna5ama.trollhack.module.modules.render

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import net.minecraft.client.network.NetworkPlayerInfo

internal object ExtraTab : Module(
    name = "Extra Tab",
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