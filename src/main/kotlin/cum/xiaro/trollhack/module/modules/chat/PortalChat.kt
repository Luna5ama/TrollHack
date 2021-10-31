package cum.xiaro.trollhack.module.modules.chat

import cum.xiaro.trollhack.mixin.player.MixinEntityPlayerSP
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module

/**
 * @see MixinEntityPlayerSP
 */
internal object PortalChat : Module(
    name = "PortalChat",
    category = Category.CHAT,
    description = "Allows you to open GUIs in portals",
    visible = false
)
