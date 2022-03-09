package me.luna.trollhack.module.modules.chat

import me.luna.trollhack.mixins.core.player.MixinEntityPlayerSP
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module

/**
 * @see MixinEntityPlayerSP
 */
internal object PortalChat : Module(
    name = "PortalChat",
    category = Category.CHAT,
    description = "Allows you to open GUIs in portals",
    visible = false
)
