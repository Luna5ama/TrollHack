package dev.luna5ama.trollhack.module.modules.chat

import dev.luna5ama.trollhack.mixins.core.player.MixinEntityPlayerSP
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module

/**
 * @see MixinEntityPlayerSP
 */
internal object PortalChat : Module(
    name = "Portal Chat",
    category = Category.CHAT,
    description = "Allows you to open GUIs in portals",
    visible = false
)